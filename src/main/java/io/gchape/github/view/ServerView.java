package io.gchape.github.view;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public enum ServerView {
    INSTANCE;

    private final BorderPane root;
    private final Label serverStatusLabel;
    private final Label connectedClientsLabel;
    private final Label serverPortLabel;
    private final TextArea logArea;
    private final Button startStopButton;

    private final StringProperty serverStatus;
    private final IntegerProperty connectedClients;
    private final StringProperty serverPort;

    private volatile boolean serverRunning = false;

    ServerView() {
        root = new BorderPane();
        serverStatus = new SimpleStringProperty("Server stopped");
        connectedClients = new SimpleIntegerProperty(0);
        serverPort = new SimpleStringProperty("Not set");

        serverStatusLabel = new Label();
        connectedClientsLabel = new Label();
        serverPortLabel = new Label();
        logArea = new TextArea();
        startStopButton = new Button("Start Server");

        addControls();
        addListeners();
    }

    private void addListeners() {
        serverStatus.addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> serverStatusLabel.setText("Status: " + newValue)));

        connectedClients.addListener((observable, oldValue, newValue) ->
                Platform.runLater(() ->
                        connectedClientsLabel.setText("Connected Clients: " + newValue.intValue())));

        serverPort.addListener((observable, oldValue, newValue) ->
                Platform.runLater(() -> serverPortLabel.setText("Port: " + newValue)));
    }

    private void addControls() {
        setupHeader();
        setupStatusPanel();
        setupLogPanel();
        setupControlPanel();
    }

    private void setupHeader() {
        Label titleLabel = new Label("Chess Server Administration");
        titleLabel.setFont(Font.font("Arial", 24));
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleLabel.getStyleClass().add("server-title");

        VBox headerBox = new VBox(titleLabel);
        GridPane.setHalignment(titleLabel, HPos.CENTER);
        GridPane.setValignment(titleLabel, VPos.CENTER);
        headerBox.setPadding(new Insets(20));
        headerBox.getStyleClass().add("server-header");

        root.setTop(headerBox);
    }

    private void setupStatusPanel() {
        VBox statusBox = new VBox(10);
        statusBox.setPadding(new Insets(20));
        statusBox.getStyleClass().add("status-panel");

        // Server info
        serverStatusLabel.setFont(Font.font("Arial", 14));
        serverStatusLabel.setStyle("-fx-font-weight: bold;");
        serverStatusLabel.getStyleClass().add("status-label");

        connectedClientsLabel.setFont(Font.font("Arial", 12));
        connectedClientsLabel.getStyleClass().add("clients-label");

        serverPortLabel.setFont(Font.font("Arial", 12));
        serverPortLabel.getStyleClass().add("port-label");

        Separator separator = new Separator();

        statusBox.getChildren().addAll(
                serverStatusLabel,
                serverPortLabel,
                connectedClientsLabel,
                separator
        );

        root.setLeft(statusBox);
    }

    private void setupLogPanel() {
        VBox logBox = new VBox(10);
        logBox.setPadding(new Insets(20));

        Label logLabel = new Label("Server Log:");
        logLabel.setFont(Font.font("Arial", 14));
        logLabel.setStyle("-fx-font-weight: bold;");

        logArea.setEditable(false);
        logArea.setPrefRowCount(20);
        logArea.getStyleClass().add("log-area");
        logArea.setWrapText(true);

        // Auto-scroll to bottom
        logArea.textProperty().addListener((observable, oldValue, newValue) -> {
            logArea.setScrollTop(Double.MAX_VALUE);
        });

        ScrollPane scrollPane = new ScrollPane(logArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        logBox.getChildren().addAll(logLabel, scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        root.setCenter(logBox);
    }

    private void setupControlPanel() {
        VBox controlBox = new VBox(15);
        controlBox.setPadding(new Insets(20));

        // Using GridPane to control alignment with HPos and VPos
        GridPane alignmentPane = new GridPane();
        alignmentPane.add(controlBox, 0, 0);
        GridPane.setHalignment(controlBox, HPos.CENTER);
        GridPane.setValignment(controlBox, VPos.TOP);

        controlBox.getStyleClass().add("control-panel");

        Label controlLabel = new Label("Server Controls:");
        controlLabel.setFont(Font.font("Arial", 14));
        controlLabel.setStyle("-fx-font-weight: bold;");

        startStopButton.setPrefWidth(120);
        startStopButton.getStyleClass().add("control-button");

        Button clearLogButton = new Button("Clear Log");
        clearLogButton.setPrefWidth(120);
        clearLogButton.getStyleClass().add("secondary-button");
        clearLogButton.setOnAction(e -> clearLog());

        Button showStatsButton = new Button("Show Stats");
        showStatsButton.setPrefWidth(120);
        showStatsButton.getStyleClass().add("secondary-button");
        showStatsButton.setOnAction(e -> showServerStats());

        controlBox.getChildren().addAll(
                controlLabel,
                startStopButton,
                clearLogButton,
                showStatsButton
        );

        root.setRight(alignmentPane);
    }

    public Region view() {
        return root;
    }

    // Property getters
    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public IntegerProperty connectedClientsProperty() {
        return connectedClients;
    }

    public StringProperty serverPortProperty() {
        return serverPort;
    }

    // UI update methods
    public void updateServerStatus(String status) {
        Platform.runLater(() -> serverStatus.set(status));
    }

    public void updateConnectedClients(int count) {
        Platform.runLater(() -> connectedClients.set(count));
    }

    public void updateServerPort(String port) {
        Platform.runLater(() -> serverPort.set(port));
    }

    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().toString();
            logArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }

    public void setServerRunning(boolean running) {
        this.serverRunning = running;
        Platform.runLater(() -> {
            if (running) {
                startStopButton.setText("Stop Server");
                startStopButton.getStyleClass().removeAll("start-button");
                startStopButton.getStyleClass().add("stop-button");
            } else {
                startStopButton.setText("Start Server");
                startStopButton.getStyleClass().removeAll("stop-button");
                startStopButton.getStyleClass().add("start-button");
            }
        });
    }

    public void setStartStopAction(Runnable action) {
        startStopButton.setOnAction(e -> action.run());
    }

    private void clearLog() {
        logArea.clear();
        addLogMessage("Log cleared");
    }

    private void showServerStats() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Server Statistics");
        alert.setHeaderText("Current Server Status");
        alert.setContentText(
                "Status: " + serverStatus.get() + "\n" +
                        "Port: " + serverPort.get() + "\n" +
                        "Connected Clients: " + connectedClients.get() + "\n" +
                        "Server Running: " + (serverRunning ? "Yes" : "No")
        );
        alert.showAndWait();
    }

    public boolean isServerRunning() {
        return serverRunning;
    }
}