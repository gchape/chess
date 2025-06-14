package io.gchape.github.view;

import io.gchape.github.controller.ChessController;
import io.gchape.github.controller.client.ClientController;
import io.gchape.github.model.Piece;
import io.gchape.github.model.PieceColor;
import io.gchape.github.model.Position;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JavaFX-based client view for the chess application.
 * Handles login, game board display, and player interactions.
 */
public class ClientView {
    private Stage primaryStage;
    private Scene loginScene;
    private Scene gameScene;
    private BorderPane gameRoot;
    private StackPane rootPane;

    // Login components
    private VBox loginPanel;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField emailField;
    private Button loginButton;
    private Button registerButton;
    private Button joinGameButton;
    private Label statusLabel;

    // Game components
    private GridPane chessBoard;
    private Button[][] boardButtons;
    private VBox gameInfoPanel;
    private Label gameStatusLabel;
    private Label currentPlayerLabel;
    private Label whiteClockLabel;
    private Label blackClockLabel;
    private TextArea moveHistoryArea;
    private Button surrenderButton;
    private Button newGameButton;
    private Button exportPGNButton;
    private Button importPGNButton;

    // Controllers
    private ClientController clientController;
    private ChessController chessController;

    // Game state
    private Position selectedPosition;
    private List<Position> legalMoves;
    private Map<String, Image> pieceImages;

    // Colors
    private static final String LIGHT_SQUARE = "#F0D9B5";
    private static final String DARK_SQUARE = "#B58863";
    private static final String SELECTED_SQUARE = "#829569";
    private static final String LEGAL_MOVE = "#829569";

    // Default constructor for compatibility with ChessApplication
    public ClientView() {
        this.boardButtons = new Button[8][8];
        this.pieceImages = new HashMap<>();
        this.rootPane = new StackPane();

        initializePieceImages();
        createLoginScene();
        createGameScene();

        // Start with login scene
        showLoginPanel();
    }

    // Legacy constructor for backward compatibility
    public ClientView(Stage primaryStage) {
        this();
        this.primaryStage = primaryStage;

        primaryStage.setTitle("Chess Client");
        primaryStage.setScene(loginScene);
        primaryStage.setResizable(false);
    }

    /**
     * Get the root view node for use in ChessApplication
     */
    public Parent view() {
        return rootPane;
    }

    /**
     * Set the primary stage (called by ChessApplication)
     */
    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Initialize chess piece images
     */
    private void initializePieceImages() {
        String[] pieces = {"white_king", "white_queen", "white_rook", "white_bishop",
                "white_knight", "white_pawn", "black_king", "black_queen",
                "black_rook", "black_bishop", "black_knight", "black_pawn"};

        for (String piece : pieces) {
            try {
                InputStream imageStream = getClass().getResourceAsStream("/images/" + piece + ".png");
                if (imageStream != null) {
                    pieceImages.put(piece, new Image(imageStream, 50, 50, true, true));
                }
            } catch (Exception e) {
                System.err.println("Could not load image for " + piece + ": " + e.getMessage());
            }
        }
    }

    /**
     * Create the login scene
     */
    private void createLoginScene() {
        loginPanel = new VBox(20);
        // Replace Pos.CENTER with manual alignment
        loginPanel.setStyle("-fx-alignment: center;");
        loginPanel.setPadding(new Insets(50));
        loginPanel.getStyleClass().add("login-panel");

        // Title
        Label titleLabel = new Label("Chess Game - Login");
        // Replace FontWeight.BOLD with CSS styling
        titleLabel.setFont(Font.font("Arial", 28));
        titleLabel.setStyle("-fx-font-weight: bold;");
        titleLabel.getStyleClass().add("title-label");

        // Login form
        VBox formBox = new VBox(15);
        formBox.setStyle("-fx-alignment: center;");
        formBox.setMaxWidth(300);

        usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.getStyleClass().add("login-field");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.getStyleClass().add("login-field");

        emailField = new TextField();
        emailField.setPromptText("Email (for registration)");
        emailField.getStyleClass().add("login-field");

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setStyle("-fx-alignment: center;");

        loginButton = new Button("Login");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setOnAction(e -> handleLogin());

        registerButton = new Button("Register");
        registerButton.getStyleClass().add("secondary-button");
        registerButton.setOnAction(e -> handleRegister());

        joinGameButton = new Button("Join Game");
        joinGameButton.getStyleClass().add("primary-button");
        joinGameButton.setOnAction(e -> handleJoinGame());
        joinGameButton.setDisable(true);

        buttonBox.getChildren().addAll(loginButton, registerButton, joinGameButton);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");

        formBox.getChildren().addAll(usernameField, passwordField, emailField, buttonBox, statusLabel);
        loginPanel.getChildren().addAll(titleLabel, formBox);
    }

    /**
     * Create the game scene
     */
    private void createGameScene() {
        gameRoot = new BorderPane();
        gameRoot.getStyleClass().add("game-root");

        // Create chess board
        createChessBoard();
        gameRoot.setCenter(chessBoard);

        // Create game info panel
        createGameInfoPanel();
        gameRoot.setRight(gameInfoPanel);

        // Create menu bar
        createMenuBar();
    }

    /**
     * Create the chess board UI
     */
    private void createChessBoard() {
        chessBoard = new GridPane();
        chessBoard.getStyleClass().add("chess-board");
        chessBoard.setStyle("-fx-alignment: center;");
        chessBoard.setPadding(new Insets(20));

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Button square = new Button();
                square.setPrefSize(70, 70);
                square.getStyleClass().add("chess-square");

                // Set square color
                if ((row + col) % 2 == 0) {
                    square.setStyle("-fx-background-color: " + LIGHT_SQUARE + ";");
                } else {
                    square.setStyle("-fx-background-color: " + DARK_SQUARE + ";");
                }

                final int finalRow = row;
                final int finalCol = col;
                square.setOnAction(e -> handleSquareClick(finalCol, 7 - finalRow)); // Flip row for proper chess notation

                boardButtons[row][col] = square;
                chessBoard.add(square, col, row);
            }
        }

        // Add rank and file labels
        addBoardLabels();
    }

    /**
     * Add rank and file labels to the chess board
     */
    private void addBoardLabels() {
        // File labels (a-h)
        for (int col = 0; col < 8; col++) {
            Label fileLabel = new Label(String.valueOf((char)('a' + col)));
            fileLabel.getStyleClass().add("board-label");
            chessBoard.add(fileLabel, col, 8);
        }

        // Rank labels (1-8)
        for (int row = 0; row < 8; row++) {
            Label rankLabel = new Label(String.valueOf(8 - row));
            rankLabel.getStyleClass().add("board-label");
            chessBoard.add(rankLabel, 8, row);
        }
    }

    /**
     * Create the game information panel
     */
    private void createGameInfoPanel() {
        gameInfoPanel = new VBox(15);
        gameInfoPanel.setPadding(new Insets(20));
        gameInfoPanel.getStyleClass().add("game-info-panel");
        gameInfoPanel.setPrefWidth(250);

        // Game status
        gameStatusLabel = new Label("Waiting for game...");
        gameStatusLabel.getStyleClass().add("game-status");
        gameStatusLabel.setWrapText(true);

        currentPlayerLabel = new Label("Current Turn: White");
        currentPlayerLabel.getStyleClass().add("current-player");

        // Clock labels
        VBox clockBox = new VBox(10);
        clockBox.getStyleClass().add("clock-box");

        whiteClockLabel = new Label("White: 10:00");
        whiteClockLabel.getStyleClass().add("clock-label");

        blackClockLabel = new Label("Black: 10:00");
        blackClockLabel.getStyleClass().add("clock-label");

        clockBox.getChildren().addAll(whiteClockLabel, blackClockLabel);

        // Move history
        Label historyLabel = new Label("Move History:");
        historyLabel.getStyleClass().add("section-label");

        moveHistoryArea = new TextArea();
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setPrefRowCount(10);
        moveHistoryArea.getStyleClass().add("move-history");

        // Control buttons
        VBox buttonBox = new VBox(10);

        surrenderButton = new Button("Surrender");
        surrenderButton.getStyleClass().add("surrender-button");
        surrenderButton.setOnAction(e -> handleSurrender());

        newGameButton = new Button("New Game");
        newGameButton.getStyleClass().add("secondary-button");
        newGameButton.setOnAction(e -> handleNewGame());

        exportPGNButton = new Button("Export PGN");
        exportPGNButton.getStyleClass().add("secondary-button");
        exportPGNButton.setOnAction(e -> handleExportPGN());

        importPGNButton = new Button("Import PGN");
        importPGNButton.getStyleClass().add("secondary-button");
        importPGNButton.setOnAction(e -> handleImportPGN());

        buttonBox.getChildren().addAll(surrenderButton, newGameButton, exportPGNButton, importPGNButton);

        gameInfoPanel.getChildren().addAll(
                gameStatusLabel, currentPlayerLabel, clockBox,
                historyLabel, moveHistoryArea, buttonBox
        );
    }

    /**
     * Create menu bar
     */
    private void createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // Game menu
        Menu gameMenu = new Menu("Game");
        MenuItem disconnectItem = new MenuItem("Disconnect");
        disconnectItem.setOnAction(e -> handleDisconnect());
        gameMenu.getItems().add(disconnectItem);

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem aboutItem = new MenuItem("About");
        aboutItem.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(gameMenu, helpMenu);
        gameRoot.setTop(menuBar);
    }

    /**
     * Handle square click on chess board
     */
    private void handleSquareClick(int x, int y) {
        if (chessController == null) return;

        Position clickedPosition = new Position(x, y);

        // If no piece is selected
        if (selectedPosition == null) {
            // Try to select a piece
            if (chessController.canSelectPiece(clickedPosition)) {
                selectedPosition = clickedPosition;
                legalMoves = chessController.getLegalMovePositions(clickedPosition);
                updateBoardHighlights();
            }
        } else {
            // If clicking the same square, deselect
            if (selectedPosition.equals(clickedPosition)) {
                clearSelection();
            } else if (legalMoves != null && legalMoves.contains(clickedPosition)) {
                // Make the move
                if (chessController.makeMove(selectedPosition, clickedPosition)) {
                    clearSelection();
                    updateBoard();
                }
            } else {
                // Try to select a different piece
                if (chessController.canSelectPiece(clickedPosition)) {
                    selectedPosition = clickedPosition;
                    legalMoves = chessController.getLegalMovePositions(clickedPosition);
                    updateBoardHighlights();
                } else {
                    clearSelection();
                }
            }
        }
    }

    /**
     * Clear piece selection
     */
    private void clearSelection() {
        selectedPosition = null;
        legalMoves = null;
        updateBoardHighlights();
    }

    /**
     * Update board square highlighting
     */
    private void updateBoardHighlights() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Button square = boardButtons[row][col];
                Position pos = new Position(col, 7 - row);

                // Reset to default color
                if ((row + col) % 2 == 0) {
                    square.setStyle("-fx-background-color: " + LIGHT_SQUARE + ";");
                } else {
                    square.setStyle("-fx-background-color: " + DARK_SQUARE + ";");
                }

                // Highlight selected square
                if (selectedPosition != null && selectedPosition.equals(pos)) {
                    square.setStyle("-fx-background-color: " + SELECTED_SQUARE + ";");
                }

                // Highlight legal moves
                if (legalMoves != null && legalMoves.contains(pos)) {
                    String currentColor = square.getStyle();
                    square.setStyle(currentColor + " -fx-border-color: " + LEGAL_MOVE + "; -fx-border-width: 3px;");
                }
            }
        }
    }

    /**
     * Update the chess board display
     */
    public void updateBoard() {
        if (chessController == null) return;

        Platform.runLater(() -> {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    Button square = boardButtons[row][col];
                    Position pos = new Position(col, 7 - row);
                    Piece piece = chessController.getPieceAt(pos);

                    // Clear previous piece image
                    square.setGraphic(null);

                    // Set piece image if present
                    if (piece != null) {
                        String imageName = getPieceImageName(piece);
                        Image pieceImage = pieceImages.get(imageName);

                        if (pieceImage != null) {
                            ImageView imageView = new ImageView(pieceImage);
                            square.setGraphic(imageView);
                        } else {
                            // Fallback: show piece letter
                            square.setText(piece.getType().substring(0, 1));
                        }
                    } else {
                        square.setText("");
                    }
                }
            }

            updateBoardHighlights();
        });
    }

    /**
     * Get the image name for a chess piece
     */
    private String getPieceImageName(Piece piece) {
        String color = piece.getColor() == PieceColor.WHITE ? "white" : "black";
        String type = piece.getType().toLowerCase();
        return color + "_" + type;
    }

    /**
     * Update game status display
     */
    public void updateGameStatus(String status) {
        Platform.runLater(() -> gameStatusLabel.setText(status));
    }

    /**
     * Update current player display
     */
    public void updateCurrentPlayer(PieceColor currentPlayer) {
        Platform.runLater(() -> currentPlayerLabel.setText("Current Turn: " + currentPlayer));
    }

    /**
     * Update clock display
     */
    public void updateClocks(String whiteTime, String blackTime) {
        Platform.runLater(() -> {
            whiteClockLabel.setText("White: " + whiteTime);
            blackClockLabel.setText("Black: " + blackTime);
        });
    }

    /**
     * Add move to history display
     */
    public void addMoveToHistory(String move) {
        Platform.runLater(() -> {
            moveHistoryArea.appendText(move + "\n");
            moveHistoryArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * Show game board scene
     */
    public void showGameBoard() {
        Platform.runLater(() -> {
            rootPane.getChildren().clear();
            rootPane.getChildren().add(gameRoot);
        });
    }

    /**
     * Show login panel
     */
    public void showLoginPanel() {
        Platform.runLater(() -> {
            rootPane.getChildren().clear();
            rootPane.getChildren().add(loginPanel);
        });
    }

    /**
     * Update login status
     */
    public void updateLoginStatus(String message, boolean isError) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            if (isError) {
                statusLabel.getStyleClass().removeAll("success-label");
                statusLabel.getStyleClass().add("error-label");
            } else {
                statusLabel.getStyleClass().removeAll("error-label");
                statusLabel.getStyleClass().add("success-label");
            }
        });
    }

    /**
     * Enable/disable join game button
     */
    public void setJoinGameEnabled(boolean enabled) {
        Platform.runLater(() -> joinGameButton.setDisable(!enabled));
    }

    // Event handlers
    private void handleLogin() {
        if (clientController != null) {
            clientController.handleLogin(usernameField.getText(), passwordField.getText());
        }
    }

    private void handleRegister() {
        if (clientController != null) {
            clientController.handleRegister(usernameField.getText(), passwordField.getText(), emailField.getText());
        }
    }

    private void handleJoinGame() {
        if (clientController != null) {
            clientController.handleJoinGame();
        }
    }

    private void handleSurrender() {
        if (chessController != null) {
            chessController.handleSurrender();
        }
    }

    private void handleNewGame() {
        if (chessController != null) {
            chessController.handleNewGame();
        }
    }

    private void handleExportPGN() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export PGN");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGN files (*.pgn)", "*.pgn"));
        File file = fileChooser.showSaveDialog(primaryStage);

        if (file != null && chessController != null) {
            chessController.handleExportPGN(file.getAbsolutePath());
        }
    }

    private void handleImportPGN() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import PGN");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PGN files (*.pgn)", "*.pgn"));
        File file = fileChooser.showOpenDialog(primaryStage);

        if (file != null && chessController != null) {
            chessController.handleImportPGN(file.getAbsolutePath());
        }
    }

    private void handleDisconnect() {
        if (clientController != null) {
            clientController.handleDisconnect();
        }
        showLoginPanel();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Chess Game Client");
        alert.setContentText("A networked chess game with database storage and PGN support.\nVersion 1.0");
        alert.showAndWait();
    }

    // Setters for controllers
    public void setClientController(ClientController clientController) {
        this.clientController = clientController;
    }

    public void setChessController(ChessController chessController) {
        this.chessController = chessController;
    }

    // Getters for UI components (for controller access)
    public String getUsername() {
        return usernameField.getText();
    }

    public String getPassword() {
        return passwordField.getText();
    }

    public String getEmail() {
        return emailField.getText();
    }
}