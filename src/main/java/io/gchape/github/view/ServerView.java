package io.gchape.github.view;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public enum ServerView {
    INSTANCE;

    private final BorderPane root;
    private final Label serverStatusLabel;
    private final Label clientCountLabel;
    private final TextArea logTextArea;

    private final StringProperty username;
    private final StringProperty password;
    private final StringProperty respMessage;
    private final StringProperty serverStatus;
    private final IntegerProperty clientCount;

    private final TextField usernameInput;
    private final PasswordField passwordInput;
    private final Button showDatabaseButton;

    ServerView() {
        root = new BorderPane();

        username = new SimpleStringProperty("");
        password = new SimpleStringProperty("");

        serverStatus = new SimpleStringProperty("");
        respMessage = new SimpleStringProperty("");
        clientCount = new SimpleIntegerProperty(0);

        serverStatusLabel = new Label();
        clientCountLabel = new Label();
        logTextArea = new TextArea();

        usernameInput = new TextField();
        passwordInput = new PasswordField();
        showDatabaseButton = new Button("Show Database");

        initializeLayout();
        bindProperties();
    }

    private void bindProperties() {
        serverStatus.addListener((obs, old, current) ->
                Platform.runLater(() -> serverStatusLabel.setText(current)));

        clientCount.addListener((obs, old, current) ->
                Platform.runLater(() ->
                        clientCountLabel.setText("Connected Clients: %d".formatted(current.intValue()))));

        respMessage.addListener((obs, old, current) ->
                Platform.runLater(() -> logTextArea.appendText(current)));

        username.bind(usernameInput.textProperty());
        password.bind(passwordInput.textProperty());
    }

    private void initializeLayout() {
        configureTopSection();
        configureCenterSection();
        configureRightSection();
    }

    private void configureTopSection() {
        VBox statusBox = new VBox();
        statusBox.getStyleClass().add("status-container");

        serverStatusLabel.getStyleClass().add("label-title");
        clientCountLabel.getStyleClass().add("label-title");

        statusBox.getChildren().addAll(serverStatusLabel, clientCountLabel);
        root.setTop(statusBox);
    }

    private void configureCenterSection() {
        logTextArea.setWrapText(true);
        logTextArea.setEditable(false);
        logTextArea.getStyleClass().add("log-area");

        root.setCenter(logTextArea);
        BorderPane.setMargin(logTextArea, new Insets(0, 8, 8, 8));
    }

    private void configureRightSection() {
        VBox authPane = new VBox();
        authPane.getStyleClass().add("auth-pane");

        Label usernameLabel = new Label("Username:");
        Label passwordLabel = new Label("Password:");

        usernameInput.setPromptText("e.g. admin");
        usernameInput.getStyleClass().add("text-field");

        passwordInput.setPromptText("enter password");
        passwordInput.getStyleClass().add("password-field");

        usernameLabel.getStyleClass().add("form-label");
        passwordLabel.getStyleClass().add("form-label");

        showDatabaseButton.setMaxWidth(Double.MAX_VALUE);

        authPane.getChildren().addAll(
                usernameLabel,
                usernameInput,
                passwordLabel,
                passwordInput,
                showDatabaseButton
        );

        root.setRight(authPane);
        BorderPane.setMargin(authPane, new Insets(0, 8, 8, 8));
    }

    public Region view() {
        return root;
    }

    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public StringProperty respMessageProperty() {
        return respMessage;
    }

    public IntegerProperty clientCountProperty() {
        return clientCount;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }
}
