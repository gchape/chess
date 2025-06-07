package io.gchape.github.view;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public enum ServerView {
    INSTANCE;

    private final BorderPane root;
    private final Label serverStatusLabel;
    private final Label connectedClientsLabel;

    private final StringProperty serverStatus;
    private final IntegerProperty connectedClients;

    ServerView() {
        root = new BorderPane();
        serverStatus = new SimpleStringProperty();
        connectedClients = new SimpleIntegerProperty();

        serverStatusLabel = new Label();
        connectedClientsLabel = new Label();

        addControls();
        addListeners();
    }

    private void addListeners() {
        serverStatus.subscribe(v -> Platform.runLater(() -> serverStatusLabel.setText(v)));

        connectedClients.subscribe(v ->
                Platform.runLater(() ->
                        connectedClientsLabel.setText("Connected clients={ %d }.".formatted(v.intValue()))));
    }

    private void addControls() {
        setupTopStatus();
        setupForms();
    }

    private void setupTopStatus() {
        VBox statusBox = new VBox();
        statusBox.getStyleClass().add("status-box");

        statusBox.getChildren().addAll(serverStatusLabel, connectedClientsLabel);
        root.setTop(statusBox);
    }

    private void setupForms() {
        HBox formsBox = new HBox();
        formsBox.getStyleClass().add("forms-box");

        VBox registrationForm = createRegistrationForm();
        VBox loginForm = createLoginForm();

        formsBox.getChildren().addAll(registrationForm, loginForm);
        root.setCenter(formsBox);
    }

    private VBox createRegistrationForm() {
        VBox form = new VBox();
        form.getStyleClass().addAll("registration-form");

        Label title = new Label("Registration");
        title.getStyleClass().add("label-title");

        TextField username = new TextField();
        username.setPromptText("Username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        TextField email = new TextField();
        email.setPromptText("Email");

        Button registerButton = new Button("Register");

        form.getChildren().addAll(title, username, password, email, registerButton);
        return form;
    }

    private VBox createLoginForm() {
        VBox form = new VBox();
        form.getStyleClass().addAll("login-form");

        Label title = new Label("Login");
        title.getStyleClass().add("label-title");

        TextField username = new TextField();
        username.setPromptText("Username");

        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        Button loginButton = new Button("Login");

        form.getChildren().addAll(title, username, password, loginButton);
        return form;
    }

    public Region view() {
        return root;
    }

    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public IntegerProperty connectedClientsProperty() {
        return connectedClients;
    }
}
