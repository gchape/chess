package io.gchape.github.view;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(5, 0, 5, 0));

        statusBox.getChildren().addAll(serverStatusLabel, connectedClientsLabel);
        root.setTop(statusBox);
    }

    private void setupForms() {
        HBox formsBox = new HBox(40);
        formsBox.setPadding(new Insets(20));
        formsBox.setAlignment(Pos.CENTER);

        VBox registrationForm = createRegistrationForm();
        VBox loginForm = createLoginForm();

        formsBox.getChildren().addAll(registrationForm, loginForm);
        root.setCenter(formsBox);
    }

    private VBox createRegistrationForm() {
        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Registration");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

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
        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER_RIGHT);

        Label title = new Label("Login");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

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
