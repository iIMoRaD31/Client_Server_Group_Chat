package com.client;

import com.client.config.ConfigLoader;
import com.client.model.ChatClient;
import com.client.model.ClientEvents;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * JavaFX client for the group chat. Handles UI only; networking is in {@link ChatClient}.
 */
public class TCPClient extends Application implements ClientEvents {
    private String defaultHost;
    private int defaultPort;

    private ChatClient chatClient;

    private TextField hostField;
    private TextField portField;
    private TextField usernameField;
    private TextArea chatArea;
    private TextField messageField;
    private Button connectButton;
    private Button sendButton;
    private Circle statusDot;
    private Label statusLabel;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        ConfigLoader config = new ConfigLoader();
        defaultHost = config.getHost();
        defaultPort = config.getPort();

        var params = getParameters().getRaw();
        if (params.size() >= 2) {
            defaultHost = params.get(0);
            try {
                defaultPort = Integer.parseInt(params.get(1));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(buildLayout(), 760, 520);
        stage.setScene(scene);
        stage.setTitle("TCP Client - Group Chat");
        stage.setOnCloseRequest(evt -> {
            if (chatClient != null) {
                chatClient.disconnect();
            }
        });
        stage.show();
    }

    private GridPane buildLayout() {
        GridPane root = new GridPane();
        root.setPadding(new Insets(16));
        root.setHgap(10);
        root.setVgap(10);

        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(70);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(30);
        root.getColumnConstraints().addAll(left, right);

        statusDot = new Circle(8, Color.RED);
        statusLabel = new Label("Offline");
        HBox statusBox = new HBox(6, statusDot, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        hostField = new TextField(defaultHost);
        hostField.setPromptText("Server IP");
        portField = new TextField(String.valueOf(defaultPort));
        usernameField = new TextField();
        usernameField.setPromptText("Username (blank = read-only)");

        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> toggleConnection());

        HBox connectionRow = new HBox(8,
                new Label("IP:"), hostField,
                new Label("Port:"), portField,
                new Label("Username:"), usernameField,
                connectButton
        );
        connectionRow.setAlignment(Pos.CENTER_LEFT);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        messageField = new TextField();
        messageField.setPromptText("Type a message, 'allUsers' for list, 'bye' to disconnect");
        messageField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });

        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        sendButton.setDisable(true);

        HBox sendRow = new HBox(8, messageField, sendButton);
        sendRow.setAlignment(Pos.CENTER_LEFT);

        root.add(statusBox, 0, 0);
        root.add(connectionRow, 0, 1, 2, 1);
        root.add(new Label("Chat"), 0, 2);
        root.add(chatArea, 0, 3, 2, 1);
        root.add(sendRow, 0, 4, 2, 1);

        return root;
    }

    private void toggleConnection() {
        if (chatClient != null && chatClient.isConnected()) {
            chatClient.disconnect();
            return;
        }

        String host = hostField.getText().trim();
        int port = parsePort(portField.getText());
        String username = usernameField.getText();

        chatClient = new ChatClient(host, port, username, this);
        chatClient.connect();

        setUiForConnection(true, chatClient.isReadOnly());
    }

    private void setUiForConnection(boolean connecting, boolean readOnly) {
        hostField.setDisable(connecting);
        portField.setDisable(connecting);
        usernameField.setDisable(connecting);
        connectButton.setText(connecting ? "Disconnect" : "Connect");
        sendButton.setDisable(!connecting || readOnly);
        messageField.setDisable(!connecting || readOnly);
        if (readOnly) {
            appendMessage("[READ-ONLY] You can view messages but cannot send.");
        }
    }

    private void sendMessage() {
        if (chatClient == null || !chatClient.isConnected()) {
            appendMessage("Not connected.");
            return;
        }
        String msg = messageField.getText().trim();
        if (msg.isEmpty()) return;
        if ("allUsers".equalsIgnoreCase(msg)) {
            chatClient.requestActiveUsers();
        } else if ("bye".equalsIgnoreCase(msg) || "end".equalsIgnoreCase(msg)) {
            chatClient.disconnect();
        } else {
            chatClient.sendMessage(msg);
        }
        messageField.clear();
    }

    private int parsePort(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            appendMessage("Invalid port, falling back to " + defaultPort);
            return defaultPort;
        }
    }

    @Override
    public void onMessage(String message) {
        Platform.runLater(() -> appendMessage(message));
    }

    @Override
    public void onStatusChanged(boolean online) {
        Platform.runLater(() -> {
            statusDot.setFill(online ? Color.LIMEGREEN : Color.RED);
            statusLabel.setText(online ? "Online" : "Offline");
            if (!online) {
                setUiForConnection(false, false);
            }
        });
    }

    private void appendMessage(String message) {
        chatArea.appendText(message + System.lineSeparator());
    }
}
