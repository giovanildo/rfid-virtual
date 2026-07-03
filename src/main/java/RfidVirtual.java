import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RfidVirtual extends Application {

    private static final int MAX_RECENT = 10;

    private TextField txtRfid;
    private FlowPane masterPane;
    private FlowPane recentPane;
    private Label lblPool;
    private Robot robot;
    private Path favoritesFile;
    private Path historyFile;

    private List<String> masterRfids = new ArrayList<>();
    private List<String> poolRfids = new ArrayList<>();
    private List<String> recentRfids = new ArrayList<>();

    @Override
    public void start(Stage stage) throws Exception {
        robot = new Robot();
        String baseDir = System.getProperty("user.dir");
        favoritesFile = Path.of(baseDir, "favorites.txt");
        historyFile = Path.of(baseDir, "history.txt");

        txtRfid = new TextField();
        txtRfid.setPromptText("Número do cartão RFID");
        txtRfid.setStyle("-fx-font-size: 16px; -fx-font-family: 'Consolas';");
        txtRfid.setPrefWidth(200);

        Button btnSend = new Button("Enviar");
        btnSend.setStyle("-fx-font-size: 14px; -fx-background-color: #2563eb; -fx-text-fill: white; "
                + "-fx-padding: 6 18; -fx-background-radius: 6; -fx-cursor: hand;");
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

        Label lblTitle = new Label("RFID Virtual");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label lblMaster = new Label("Master:");
        lblMaster.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
        lblMaster.setMinWidth(55);
        masterPane = new FlowPane(6, 4);
        HBox masterRow = new HBox(8, lblMaster, masterPane);
        masterRow.setAlignment(Pos.CENTER_LEFT);

        Label lblRecent = new Label("Recentes:");
        lblRecent.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        lblRecent.setMinWidth(55);
        recentPane = new FlowPane(6, 4);
        HBox recentRow = new HBox(8, lblRecent, recentPane);
        recentRow.setAlignment(Pos.CENTER_LEFT);

        lblPool = new Label();
        lblPool.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        VBox root = new VBox(8, lblTitle, inputRow, masterRow, recentRow, lblPool);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; "
                + "-fx-border-radius: 8; -fx-background-radius: 8;");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("RFID Virtual");
        stage.setAlwaysOnTop(true);
        stage.setWidth(480);
        stage.setHeight(220);
        stage.show();

        loadFavorites();
        loadHistory();
        txtRfid.requestFocus();
    }

    private void sendRfid() {
        String rfid = txtRfid.getText().trim();
        if (rfid.isEmpty()) return;

        addToHistory(rfid);

        Stage myStage = (Stage) txtRfid.getScene().getWindow();
        myStage.setIconified(true);

        new Thread(() -> {
            try {
                Thread.sleep(300);
                typeString(rfid);
                robot.keyPress(KeyEvent.VK_ENTER);
                robot.keyRelease(KeyEvent.VK_ENTER);

                Thread.sleep(500);
                Platform.runLater(() -> {
                    myStage.setIconified(false);
                    myStage.toFront();
                    txtRfid.selectAll();
                    txtRfid.requestFocus();
                });
            } catch (InterruptedException ignored) {}
        }, "rfid-send").start();
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
        masterRfids.clear();
        poolRfids.clear();

        try {
            if (!Files.exists(favoritesFile)) return;

            String section = "pool";
            for (String line : Files.readAllLines(favoritesFile)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (trimmed.startsWith("#")) {
                    String header = trimmed.substring(1).trim().toLowerCase();
                    if (header.contains("master")) section = "master";
                    else section = "pool";
                    continue;
                }

                switch (section) {
                    case "master" -> masterRfids.add(trimmed);
                    default -> poolRfids.add(trimmed);
                }
            }
        } catch (IOException ignored) {}

        renderSection(masterPane, masterRfids,
                "-fx-background-color: #fecaca; -fx-text-fill: #991b1b;");
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

    public static void main(String[] args) {
        launch(args);
    }
}
