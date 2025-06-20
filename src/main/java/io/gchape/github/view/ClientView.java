package io.gchape.github.view;

import io.gchape.github.controller.handlers.MouseClickHandlers;
import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.*;

public class ClientView {
    private static final int TILE_SIZE = 80;
    private static final int BOARD_SIZE = 8;
    private static final Color LIGHT_SQUARE = Color.WHEAT;
    private static final Color DARK_SQUARE = Color.SADDLEBROWN;

    public final List<StackPane> highlightedSquares = new ArrayList<>();
    private final Map<Piece, Image> pieceImages = new HashMap<>();
    private final BorderPane rootLayout;

    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty password = new SimpleStringProperty();

    public Position selectedPosition;

    private GridPane board;
    private Button loginButton;
    private Button guestLoginButton;
    private Button registerButton;

    private MouseClickHandlers mouseClickHandlers;

    public ClientView() {
        rootLayout = new BorderPane();
        rootLayout.setCenter(createFormsContainer());
        rootLayout.setBottom(createGuestLoginContainer());
    }

    private HBox createGuestLoginContainer() {
        var guestLoginButton = createGuestLoginButton();
        var guestLoginContainer = new HBox(guestLoginButton);

        guestLoginContainer.getStyleClass().add("guest-login-container");
        HBox.setMargin(guestLoginButton, new Insets(0, 0, 8, 0));

        return guestLoginContainer;
    }

    public Region view() {
        return rootLayout;
    }

    private void loadPieceImages() {
        for (Piece piece : Piece.values()) {
            try {
                final Image image = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/" + piece.imageName() + ".png"))
                );
                pieceImages.put(piece, image);
            } catch (Exception e) {
                System.err.println("Failed to load image for piece: " + piece);
            }
        }
    }

    private GridPane setupBoard() {
        final GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                grid.add(createSquare(row, col), col, row);
            }
        }

        return grid;
    }

    private HBox createFormsContainer() {
        final HBox formsBox = new HBox();
        formsBox.getStyleClass().add("forms-container");

        final VBox registrationForm = createRegistrationForm();
        final VBox loginForm = createLoginForm();

        formsBox.getChildren().addAll(registrationForm, loginForm);
        return formsBox;
    }

    private Button createGuestLoginButton() {
        guestLoginButton = new Button("Guest Login");
        guestLoginButton.getStyleClass().add("guest-login-button");

        return guestLoginButton;
    }

    private VBox createRegistrationForm() {
        final VBox registrationForm = new VBox();
        registrationForm.getStyleClass().add("registration-form");

        final Label title = new Label("Registration");
        title.getStyleClass().add("label-title");

        final TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        username.bind(usernameField.textProperty());

        final PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        password.bind(passwordField.textProperty());

        final TextField emailField = new TextField();
        emailField.setPromptText("Email");
        email.bind(emailField.textProperty());

        registerButton = new Button("Register");

        registrationForm.getChildren().addAll(title, usernameField, passwordField, emailField, registerButton);
        return registrationForm;
    }

    private VBox createLoginForm() {
        final VBox loginForm = new VBox();
        loginForm.getStyleClass().add("login-form");

        final Label title = new Label("Login");
        title.getStyleClass().add("label-title");

        final TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        username.bind(usernameField.textProperty());

        final PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        password.bind(passwordField.textProperty());

        loginButton = new Button("Login");

        loginForm.getChildren().addAll(title, usernameField, passwordField, loginButton);
        return loginForm;
    }

    private StackPane createSquare(int row, int col) {
        final Rectangle tile = new Rectangle(TILE_SIZE, TILE_SIZE);
        tile.setFill((row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE);

        final StackPane square = new StackPane(tile);
        square.setUserData(new Position(row, col));

        square.setOnMouseClicked(mouseClickHandlers::onSquareClicked);

        return square;
    }

    private void addPieceToSquare(StackPane square, Piece piece) {
        final Image image = pieceImages.get(piece);
        if (image == null) return;

        final ImageView view = new ImageView(image);
        view.setSmooth(true);
        view.setFitWidth(TILE_SIZE * 0.5);
        view.setFitHeight(TILE_SIZE * 0.5);

        square.getChildren().add(view);
    }

    private void removePieceFromSquare(StackPane square) {
        square.getChildren().removeIf(node -> node instanceof ImageView);
    }

    public GridPane createBoard() {
        loadPieceImages();
        board = setupBoard();
        board.getStyleClass().add("grid-pane");

        return board;
    }

    public void updateBoard(final GameState gameState) {
        for (Node node : board.getChildren()) {
            if (node instanceof StackPane square) {
                final Position pos = (Position) square.getUserData();

                removePieceFromSquare(square);

                final Piece piece = gameState.getPieceAt(pos);
                if (piece != null) {
                    addPieceToSquare(square, piece);
                }
            }
        }
    }

    public void setMouseClickHandlers(final MouseClickHandlers handlers) {
        this.mouseClickHandlers = handlers;

        registerButton.setOnMouseClicked(handlers::onRegisterClicked);
        loginButton.setOnMouseClicked(handlers::onLoginClicked);
        guestLoginButton.setOnMouseClicked(handlers::onGuestClicked);
    }

    public GridPane getBoard() {
        return board;
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }
}
