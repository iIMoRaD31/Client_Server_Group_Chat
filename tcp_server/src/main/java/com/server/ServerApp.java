package com.server;

import com.server.config.ConfigLoader;
import com.server.core.ChatServer;
import com.server.core.ServerEvents;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaFX view/controller for the TCP chat server.
 * Networking lives in {@link ChatServer}; this class only reacts to events.
 */
public class ServerApp extends Application implements ServerEvents {
    private final ObservableList<String> users = FXCollections.observableArrayList();
    private final Map<String, String> userColors = new ConcurrentHashMap<>();

    private ChatServer chatServer;
    private Circle statusDot;
    private Label statusLabel;
    private TextArea logArea;
    private ListView<String> userListView;

    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        ConfigLoader config = new ConfigLoader();
        chatServer = new ChatServer(config.getHost(), config.getPort(), this);

        Scene scene = new Scene(buildLayout(), 760, 520);
        stage.setScene(scene);
        stage.setTitle("TCP Server - Group Chat");
        stage.setOnCloseRequest(evt -> chatServer.stop());
        stage.show();

        chatServer.start();
    }

    private GridPane buildLayout() {
        GridPane root = new GridPane();
        root.setPadding(new Insets(16));
        root.setHgap(12);
        root.setVgap(12);

        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(70);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(30);
        root.getColumnConstraints().addAll(left, right);

        statusDot = new Circle(8, Color.RED);
        statusLabel = new Label("Offline");
        HBox statusBox = new HBox(8, statusDot, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Button stopButton = new Button("Stop Server");
        stopButton.setOnAction(e -> chatServer.stop());
        HBox header = new HBox(12, statusBox, stopButton);
        header.setAlignment(Pos.CENTER_LEFT);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);

        userListView = new ListView<>(users);
        userListView.setPlaceholder(new Label("No connected users yet"));
        userListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    String color = userColors.getOrDefault(item, "#dddddd");
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: black;");
                }
            }
        });

        root.add(header, 0, 0, 2, 1);
        root.add(new Label("Server Log"), 0, 1);
        root.add(new Label("Active Users"), 1, 1);
        root.add(logArea, 0, 2);
        root.add(userListView, 1, 2);

        GridPane.setVgrow(logArea, Priority.ALWAYS);
        GridPane.setVgrow(userListView, Priority.ALWAYS);
        GridPane.setHgrow(logArea, Priority.ALWAYS);
        GridPane.setHgrow(userListView, Priority.ALWAYS);

        return root;
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + System.lineSeparator());
        });
    }

    @Override
    public void onUsersChanged(Map<String, String> colors) {
        Platform.runLater(() -> {
            users.setAll(colors.keySet());
            userColors.clear();
            userColors.putAll(colors);
        });
    }

    @Override
    public void onServerStatus(boolean online) {
        Platform.runLater(() -> {
            statusDot.setFill(online ? Color.LIMEGREEN : Color.RED);
            statusLabel.setText(online ? "Online" : "Offline");
        });
    }
}
