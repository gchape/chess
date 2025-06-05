package io.gchape.github.view;

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
    private final StringProperty serverUpdates;
    private final IntegerProperty clientCount;

    ServerView() {
        root = new BorderPane();
        serverUpdates = new SimpleStringProperty();
        clientCount = new SimpleIntegerProperty(0);

        setupControls();
    }

    private void setupControls() {
        setupTopStatus();
        setupForms();
    }

    private void setupTopStatus() {
        VBox statusBox = new VBox(5);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPadding(new Insets(5, 0, 5, 0));

        Label statusLabel = new Label();
        Label clientsLabel = new Label();

        statusLabel.textProperty().bind(serverUpdates);
        clientsLabel.textProperty().bind(clientCount.asString("Connected clients={ %d }."));

        statusBox.getChildren().addAll(statusLabel, clientsLabel);
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

    public StringProperty serverUpdatesProperty() {
        return serverUpdates;
    }

    public IntegerProperty clientCountProperty() {
        return clientCount;
    }
}
