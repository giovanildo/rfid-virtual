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

public class RfidVirtual extends Application {

    private static final int MAX_FAVORITES = 10;

    private TextField txtRfid;
    private FlowPane favoritesPane;
    private Robot robot;
    private Path favoritesFile;

    @Override
    public void start(Stage stage) throws Exception {
        robot = new Robot();
        favoritesFile = Path.of(System.getProperty("user.dir"), "favorites.txt");

        txtRfid = new TextField();
        txtRfid.setPromptText("Número do cartão RFID");
        txtRfid.setStyle("-fx-font-size: 16px; -fx-font-family: 'Consolas';");
        txtRfid.setPrefWidth(220);

        Button btnSend = new Button("Enviar");
        btnSend.setStyle("-fx-font-size: 14px; -fx-background-color: #2563eb; -fx-text-fill: white; "
                + "-fx-padding: 6 18; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSend.setOnAction(e -> sendRfid());
        btnSend.setFocusTraversable(false);

        Button btnSave = new Button("★");
        btnSave.setTooltip(new Tooltip("Salvar como favorito"));
        btnSave.setStyle("-fx-font-size: 14px; -fx-padding: 6 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSave.setOnAction(e -> saveFavorite());
        btnSave.setFocusTraversable(false);

        txtRfid.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendRfid();
        });

        HBox inputRow = new HBox(8, txtRfid, btnSend, btnSave);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        Label lblTitle = new Label("RFID Virtual");
        lblTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label lblFavLabel = new Label("Favoritos:");
        lblFavLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        favoritesPane = new FlowPane(6, 6);
        favoritesPane.setPadding(new Insets(2, 0, 0, 0));

        VBox root = new VBox(10, lblTitle, inputRow, lblFavLabel, favoritesPane);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; "
                + "-fx-border-radius: 8; -fx-background-radius: 8;");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("RFID Virtual");
        stage.setAlwaysOnTop(true);
        stage.setWidth(380);
        stage.setHeight(200);
        stage.show();

        loadFavorites();
        txtRfid.requestFocus();
    }

    private void sendRfid() {
        String rfid = txtRfid.getText().trim();
        if (rfid.isEmpty()) return;

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

    private List<String> readFavorites() {
        try {
            if (Files.exists(favoritesFile)) {
                return new ArrayList<>(Files.readAllLines(favoritesFile).stream()
                        .filter(s -> !s.isBlank())
                        .toList());
            }
        } catch (IOException ignored) {}
        return new ArrayList<>();
    }

    private void writeFavorites(List<String> favorites) {
        try {
            Files.writeString(favoritesFile, String.join("\n", favorites) + "\n");
        } catch (IOException ignored) {}
    }

    private void saveFavorite() {
        String rfid = txtRfid.getText().trim();
        if (rfid.isEmpty()) return;

        List<String> favorites = readFavorites();
        if (favorites.contains(rfid)) return;

        favorites.add(rfid);
        if (favorites.size() > MAX_FAVORITES) {
            favorites = favorites.subList(favorites.size() - MAX_FAVORITES, favorites.size());
        }
        writeFavorites(favorites);
        loadFavorites();
    }

    private void removeFavorite(String rfid) {
        List<String> favorites = readFavorites();
        favorites.remove(rfid);
        writeFavorites(favorites);
        loadFavorites();
    }

    private void loadFavorites() {
        favoritesPane.getChildren().clear();

        for (String rfid : readFavorites()) {
            Button btn = new Button(rfid);
            btn.setStyle("-fx-font-size: 12px; -fx-font-family: 'Consolas'; "
                    + "-fx-background-color: #e2e8f0; -fx-background-radius: 12; "
                    + "-fx-padding: 4 12; -fx-cursor: hand;");
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> {
                txtRfid.setText(rfid);
                sendRfid();
            });

            btn.setContextMenu(createFavContextMenu(rfid));
            favoritesPane.getChildren().add(btn);
        }
    }

    private ContextMenu createFavContextMenu(String rfid) {
        MenuItem remove = new MenuItem("Remover");
        remove.setOnAction(e -> removeFavorite(rfid));
        return new ContextMenu(remove);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
