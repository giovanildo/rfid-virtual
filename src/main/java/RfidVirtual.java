import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;
import javafx.stage.Screen;

import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Arc2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RfidVirtual extends Application {

    private static final int MAX_RECENT = 10;
    private static final boolean IS_WINDOWS =
            System.getProperty("os.name", "").toLowerCase().contains("win");

    private TextField txtRfid;
    private FlowPane masterAcessoPane;
    private FlowPane masterRecargaPane;
    private FlowPane recentPane;
    private Label lblPool;
    private Robot robot;
    private Path favoritesFile;
    private Path historyFile;

    private List<String> masterAcessoRfids = new ArrayList<>();
    private List<String> masterRecargaRfids = new ArrayList<>();
    private List<String> poolRfids = new ArrayList<>();
    private List<String> recentRfids = new ArrayList<>();
    private Stage mainStage;

    /** Última janela externa (de outro processo) que teve o foco — alvo para devolver o foco. */
    private volatile HWND lastForegroundHwnd;
    private long ownPid;
    private java.util.Timer focusTracker;

    @Override
    public void start(Stage stage) throws Exception {
        this.mainStage = stage;
        Platform.setImplicitExit(false);
        robot = new Robot();
        ownPid = ProcessHandle.current().pid();
        if (IS_WINDOWS) startForegroundTracker();
        String baseDir = System.getProperty("user.dir");
        favoritesFile = Path.of(baseDir, "favorites.txt");
        historyFile = Path.of(baseDir, "history.txt");

        txtRfid = new TextField();
        txtRfid.setPromptText("Número do cartão RFID");
        txtRfid.setStyle("-fx-font-size: 16px; -fx-font-family: 'Consolas';");
        txtRfid.setPrefWidth(200);

        Button btnSend = new Button("Enviar");
        btnSend.setStyle("-fx-font-size: 14px; -fx-background-color: #2563eb; -fx-text-fill: white; -fx-padding: 6 18; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSend.setOnAction(e -> sendRfid());
        btnSend.setFocusTraversable(false);

        Button btnRandom = new Button("🎲 Aleatório");
        btnRandom.setTooltip(new Tooltip("RFID aleatório do pool"));
        btnRandom.setStyle("-fx-font-size: 13px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRandom.setOnAction(e -> pickRandomAndSend());
        btnRandom.setFocusTraversable(false);

        txtRfid.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendRfid();
        });

        HBox inputRow = new HBox(8, txtRfid, btnSend, btnRandom);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("RFID Virtual (Expandido)");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label lblMasterAcesso = new Label("M. Acesso:");
        lblMasterAcesso.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
        lblMasterAcesso.setMinWidth(65);
        masterAcessoPane = new FlowPane(6, 4);
        HBox masterAcessoRow = new HBox(8, lblMasterAcesso, masterAcessoPane);
        masterAcessoRow.setAlignment(Pos.CENTER_LEFT);

        Label lblMasterRecarga = new Label("M. Recarga:");
        lblMasterRecarga.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #d97706;");
        lblMasterRecarga.setMinWidth(65);
        masterRecargaPane = new FlowPane(6, 4);
        HBox masterRecargaRow = new HBox(8, lblMasterRecarga, masterRecargaPane);
        masterRecargaRow.setAlignment(Pos.CENTER_LEFT);

        Label lblRecent = new Label("Recentes:");
        lblRecent.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        lblRecent.setMinWidth(65);
        recentPane = new FlowPane(6, 4);
        HBox recentRow = new HBox(8, lblRecent, recentPane);
        recentRow.setAlignment(Pos.CENTER_LEFT);

        lblPool = new Label();
        lblPool.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        Button btnCollapse = new Button("➖ Recolher");
        btnCollapse.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-cursor: hand;");
        HBox titleRow = new HBox(lblTitle, new Region(), btnCollapse);
        HBox.setHgrow(titleRow.getChildren().get(1), Priority.ALWAYS);

        VBox fullRoot = new VBox(10, titleRow, inputRow, masterAcessoRow, masterRecargaRow, recentRow, lblPool);
        fullRoot.setPadding(new Insets(14));
        fullRoot.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        // --- MINI WIDGET BAR ---
        Button btnMiniAcesso = new Button("M. Acesso");
        btnMiniAcesso.setStyle("-fx-background-color: #fecaca; -fx-text-fill: #991b1b; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 6 12;");
        btnMiniAcesso.setOnAction(e -> {
            if (!masterAcessoRfids.isEmpty()) {
                txtRfid.setText(masterAcessoRfids.get(0));
                sendRfid();
            }
        });

        Button btnMiniRecarga = new Button("M. Recarga");
        btnMiniRecarga.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 6 12;");
        btnMiniRecarga.setOnAction(e -> {
            if (!masterRecargaRfids.isEmpty()) {
                txtRfid.setText(masterRecargaRfids.get(0));
                sendRfid();
            }
        });

        Button btnMiniRandom = new Button("🎲 Aleatório");
        btnMiniRandom.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 6 12;");
        btnMiniRandom.setOnAction(e -> pickRandomAndSend());
        
        Button btnExpand = new Button("➕");
        btnExpand.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-cursor: hand; -fx-font-weight: bold;");
        
        Button btnClose = new Button("❌");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-cursor: hand;");
        btnClose.setOnAction(e -> stage.hide());
        
        Label dragHandle = new Label(" ⣿ ");
        dragHandle.setStyle("-fx-text-fill: #94a3b8; -fx-cursor: move; -fx-font-size: 16px;");

        HBox miniBar = new HBox(6, dragHandle, btnMiniAcesso, btnMiniRecarga, btnMiniRandom, new Region(), btnExpand, btnClose);
        miniBar.setAlignment(Pos.CENTER);
        miniBar.setPadding(new Insets(6, 6, 6, 0));
        miniBar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        StackPane mainRoot = new StackPane(miniBar);
        mainRoot.setStyle("-fx-background-color: transparent; -fx-padding: 10;");

        btnExpand.setOnAction(e -> {
            mainRoot.getChildren().setAll(fullRoot);
            stage.sizeToScene();
        });
        
        btnCollapse.setOnAction(e -> {
            mainRoot.getChildren().setAll(miniBar);
            stage.sizeToScene();
        });

        final double[] xOffset = new double[1];
        final double[] yOffset = new double[1];
        
        mainRoot.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        mainRoot.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset[0]);
            stage.setY(event.getScreenY() - yOffset[0]);
        });

        Scene scene = new Scene(mainRoot, Color.TRANSPARENT);
        stage.setScene(scene);
        stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        stage.setTitle("RFID Virtual");
        stage.getIcons().add(createRfidIcon());
        stage.setAlwaysOnTop(true);
        
        stage.setOnCloseRequest(e -> {
            e.consume();
            stage.hide();
        });
        setupSystemTray(stage);

        loadFavorites();
        loadHistory();
        
        stage.setOnShown(e -> {
            javafx.geometry.Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(screenBounds.getMaxX() - stage.getWidth() - 10);
            stage.setY(screenBounds.getMaxY() - stage.getHeight() - 10);
        });

        stage.show();
        txtRfid.requestFocus();
    }

    private void sendRfid() {
        String rfid = txtRfid.getText().trim();
        if (rfid.isEmpty()) return;

        addToHistory(rfid);

        final HWND target = lastForegroundHwnd;
        final boolean hasTarget = IS_WINDOWS && target != null;

        // Sem alvo (não-Windows ou sem janela anterior conhecida): fallback antigo
        // — minimiza para liberar o foco e restaura a própria janela depois.
        if (!hasTarget) mainStage.setIconified(true);

        new Thread(() -> {
            try {
                if (hasTarget) {
                    // Devolve o foco à aplicação em que o usuário estava antes de clicar.
                    // Só é permitido porque, no momento do clique, NÓS somos o processo
                    // em foreground — o Windows autoriza SetForegroundWindow nesse caso.
                    restoreForeground(target);
                    Thread.sleep(120);
                } else {
                    Thread.sleep(300);
                }

                typeString(rfid);
                robot.keyPress(KeyEvent.VK_ENTER);
                robot.keyRelease(KeyEvent.VK_ENTER);

                if (!hasTarget) {
                    Thread.sleep(500);
                    Platform.runLater(() -> {
                        mainStage.setIconified(false);
                        mainStage.toFront();
                        if (txtRfid.getScene() != null) {
                            txtRfid.selectAll();
                            txtRfid.requestFocus();
                        }
                    });
                }
                // Com alvo: NÃO trazemos a janela de volta — o foco fica na app de destino.
            } catch (InterruptedException ignored) {}
        }, "rfid-send").start();
    }

    /**
     * Poller que memoriza, a cada 150ms, qual janela de OUTRO processo está em foco.
     * No instante em que o usuário clica num botão do RFID Virtual, {@link #lastForegroundHwnd}
     * já guarda a janela anterior — para onde o foco será devolvido no envio.
     */
    private void startForegroundTracker() {
        focusTracker = new java.util.Timer("fg-tracker", true);
        focusTracker.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() {
                try {
                    User32 user32 = User32.INSTANCE;
                    HWND hwnd = user32.GetForegroundWindow();
                    if (hwnd == null) return;
                    IntByReference pidRef = new IntByReference();
                    user32.GetWindowThreadProcessId(hwnd, pidRef);
                    if (pidRef.getValue() != ownPid) {
                        lastForegroundHwnd = hwnd;
                    }
                } catch (Throwable ignored) {}
            }
        }, 0, 150);
    }

    /** Traz a janela alvo para o foreground (a app em que o usuário estava). */
    private void restoreForeground(HWND hwnd) {
        User32.INSTANCE.SetForegroundWindow(hwnd);
    }

    private void pickRandomAndSend() {
        if (poolRfids.isEmpty()) return;
        int idx = ThreadLocalRandom.current().nextInt(poolRfids.size());
        txtRfid.setText(poolRfids.get(idx));
        sendRfid();
    }

    private void typeString(String text) {
        for (char c : text.toCharArray()) {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (keyCode != KeyEvent.VK_UNDEFINED) {
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void loadFavorites() {
        masterAcessoRfids.clear();
        masterRecargaRfids.clear();
        poolRfids.clear();

        try {
            if (!Files.exists(favoritesFile)) return;

            String section = "pool";
            for (String line : Files.readAllLines(favoritesFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (trimmed.startsWith("#")) {
                    String header = trimmed.substring(1).trim().toLowerCase();
                    if (header.contains("acesso")) {
                        section = "acesso";
                    } else if (header.contains("recarga")) {
                        section = "recarga";
                    } else {
                        section = "pool";
                    }
                    continue;
                }

                switch (section) {
                    case "acesso" -> masterAcessoRfids.add(trimmed);
                    case "recarga" -> masterRecargaRfids.add(trimmed);
                    default -> poolRfids.add(trimmed);
                }
            }
        } catch (IOException ignored) {}

        // Renderiza Master Acesso (Vermelho)
        renderSection(masterAcessoPane, masterAcessoRfids,
                "-fx-background-color: #fecaca; -fx-text-fill: #991b1b;");

        // Renderiza Master Recarga (Laranja/Amarelo escuro)
        renderSection(masterRecargaPane, masterRecargaRfids,
                "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;");

        lblPool.setText("Pool: " + poolRfids.size() + " RFIDs disponíveis para aleatório");
    }

    private void loadHistory() {
        recentRfids.clear();
        try {
            if (Files.exists(historyFile)) {
                recentRfids = new ArrayList<>(Files.readAllLines(historyFile).stream()
                        .filter(s -> !s.isBlank())
                        .toList());
            }
        } catch (IOException ignored) {}
        renderSection(recentPane, recentRfids,
                "-fx-background-color: #bfdbfe; -fx-text-fill: #1e3a8a;");
    }

    private void addToHistory(String rfid) {
        // Se já for um cartão master, não joga para os recentes
        if (masterAcessoRfids.contains(rfid) || masterRecargaRfids.contains(rfid)) return;

        recentRfids.remove(rfid);
        recentRfids.addFirst(rfid);
        if (recentRfids.size() > MAX_RECENT) {
            recentRfids = new ArrayList<>(recentRfids.subList(0, MAX_RECENT));
        }
        try {
            Files.writeString(historyFile, String.join("\n", recentRfids) + "\n");
        } catch (IOException ignored) {}
        renderSection(recentPane, recentRfids,
                "-fx-background-color: #bfdbfe; -fx-text-fill: #1e3a8a;");
    }

    private void renderSection(FlowPane pane, List<String> rfids, String colorStyle) {
        pane.getChildren().clear();
        for (String rfid : rfids) {
            Button btn = new Button(rfid);
            btn.setStyle("-fx-font-size: 12px; -fx-font-family: 'Consolas'; "
                    + colorStyle + " -fx-background-radius: 12; "
                    + "-fx-padding: 4 12; -fx-cursor: hand;");
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> {
                txtRfid.setText(rfid);
                sendRfid();
            });
            pane.getChildren().add(btn);
        }
    }

    private Image createRfidIcon() {
        int size = 64;
        Canvas canvas = new Canvas(size, size);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.web("#2563eb"));
        gc.fillRoundRect(4, 12, 56, 40, 8, 8);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.5);
        gc.strokeArc(34, 22, 14, 14, -45, 90, ArcType.OPEN);
        gc.strokeArc(30, 18, 22, 22, -45, 90, ArcType.OPEN);
        gc.strokeArc(26, 14, 30, 30, -45, 90, ArcType.OPEN);

        gc.setFill(Color.WHITE);
        gc.fillOval(36, 28, 5, 5);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return canvas.snapshot(params, new WritableImage(size, size));
    }

    private void setupSystemTray(Stage stage) {
        if (!SystemTray.isSupported()) return;

        SystemTray tray = SystemTray.getSystemTray();
        java.awt.Image image = createAwtTrayIcon();

        PopupMenu popup = new PopupMenu();
        
        MenuItem openItem = new MenuItem("Abrir");
        openItem.addActionListener(e -> Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        }));
        
        MenuItem exitItem = new MenuItem("Sair");
        exitItem.addActionListener(e -> System.exit(0));

        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(image, "RFID Virtual", popup);
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    Platform.runLater(() -> {
                        stage.show();
                        stage.toFront();
                    });
                }
            }
        });

        try {
            tray.add(trayIcon);
            System.out.println("✅ [RFID Virtual] Ícone adicionado à Bandeja do Sistema (System Tray) com sucesso!");
            System.out.println("👉 Feche esta janela no 'X' e ela continuará rodando perto do relógio do Windows.");
        } catch (java.awt.AWTException e) {
            System.out.println("❌ Erro ao adicionar ícone na bandeja: " + e.getMessage());
        }
    }

    private java.awt.Image createAwtTrayIcon() {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(java.awt.Color.decode("#2563eb"));
        g2.fill(new RoundRectangle2D.Float(1, 3, 14, 10, 2, 2));

        g2.setColor(java.awt.Color.WHITE);
        g2.setStroke(new BasicStroke(1.0f));
        g2.draw(new Arc2D.Float(8.5f, 5.5f, 3.5f, 3.5f, -45, 90, Arc2D.OPEN));
        g2.draw(new Arc2D.Float(7.5f, 4.5f, 5.5f, 5.5f, -45, 90, Arc2D.OPEN));
        g2.draw(new Arc2D.Float(6.5f, 3.5f, 7.5f, 7.5f, -45, 90, Arc2D.OPEN));

        g2.fillOval(9, 7, 2, 2);
        g2.dispose();
        
        return img;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
