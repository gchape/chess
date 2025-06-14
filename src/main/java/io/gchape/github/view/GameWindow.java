package io.gchape.github.view;

import io.gchape.github.controller.ChessController;
import io.gchape.github.model.Clock;
import io.gchape.github.model.PieceColor;
import io.gchape.github.model.entity.ClientMode;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * JavaFX-based main window frame for the Chess game application.
 * Serves as the primary container that holds all UI components.
 * Updated for network play support and JavaFX compatibility.
 */
public class GameWindow {
    private ChessBoardUI chessBoardUI;
    private Clock whiteClock;
    private Clock blackClock;
    private Label whiteClockLabel;
    private Label blackClockLabel;
    private Timeline clockTimer;
    private VBox sidePanel;
    private VBox gameInfoPanel;
    private Label statusLabel;
    private Label gameTypeLabel;
    private Label playerInfoLabel;
    private Button newGameButton;
    private Button surrenderButton;
    private Button disconnectButton;
    private ChessController controller;
    private StartMenu startMenu;
    private Stage primaryStage;
    private Scene gameScene;
    private BorderPane gameRoot;

    // Network game properties
    private boolean isNetworkGame = false;
    private ClientMode clientMode = ClientMode.PLAYER;
    private PieceColor playerColor = PieceColor.WHITE;
    private String gameMode = "Local Game";

    /**
     * Constructs a new GameWindow with the given controller.
     *
     * @param controller The chess game controller
     */
    public GameWindow(ChessController controller) {
        this.controller = controller;
        controller.setView(this);
    }

    /**
     * Initialize the GameWindow with a primary stage.
     *
     * @param primaryStage The primary JavaFX stage
     */
    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize components
        initializeComponents();

        // Display start menu initially
        showStartMenu();

        // Set stage properties
        primaryStage.setTitle("Chess Game");
        primaryStage.setWidth(1100);
        primaryStage.setHeight(800);
        primaryStage.centerOnScreen();

        // Handle window close
        primaryStage.setOnCloseRequest(e -> {
            if (clockTimer != null) {
                clockTimer.stop();
            }
            if (controller != null) {
                controller.disconnect();
            }
            Platform.exit();
        });
    }

    /**
     * Initializes the game clocks.
     */
    private void initializeClocks(int hours, int minutes, int seconds) {
        whiteClock = new Clock(hours, minutes, seconds);
        blackClock = new Clock(hours, minutes, seconds);

        updateClockLabels();

        // Start the clock timer
        startClockTimer();
    }

    /**
     * Initialize all UI components.
     */
    private void initializeComponents() {
        // Create the chess board UI
        chessBoardUI = new ChessBoardUI(controller);

        // Create side panel with game controls
        createSidePanel();

        // Create game scene
        createGameScene();

        // Create start menu
        startMenu = new StartMenu(primaryStage);
    }

    /**
     * Creates the main game scene.
     */
    private void createGameScene() {
        gameRoot = new BorderPane();

        // Add chess board to center
        gameRoot.setCenter(chessBoardUI);

        // Add side panel to right
        gameRoot.setRight(sidePanel);

        // Create scene
        gameScene = new Scene(gameRoot, 1100, 800);

        // Add CSS styling
        String css = """
            .game-root {
                -fx-background-color: #f5f5f5;
            }
            
            .side-panel {
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-border-width: 0 0 0 1;
                -fx-padding: 20;
                -fx-spacing: 15;
                -fx-min-width: 280;
                -fx-max-width: 280;
            }
            
            .info-panel {
                -fx-background-color: #f8f9fa;
                -fx-border-color: #dee2e6;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
                -fx-border-width: 1;
                -fx-padding: 15;
                -fx-spacing: 8;
            }
            
            .clock-panel {
                -fx-background-color: #e9ecef;
                -fx-border-color: #adb5bd;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
                -fx-border-width: 1;
                -fx-padding: 15;
                -fx-spacing: 10;
            }
            
            .clock-label {
                -fx-font-family: "Courier New", monospace;
                -fx-font-size: 16px;
                -fx-font-weight: bold;
            }
            
            .white-clock {
                -fx-text-fill: #2c3e50;
            }
            
            .black-clock {
                -fx-text-fill: #34495e;
            }
            
            .status-label {
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-text-fill: #2980b9;
                -fx-text-alignment: center;
            }
            
            .game-button {
                -fx-font-size: 13px;
                -fx-padding: 8 16 8 16;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
                -fx-min-width: 120;
            }
            
            .primary-button {
                -fx-background-color: #007bff;
                -fx-text-fill: white;
            }
            
            .primary-button:hover {
                -fx-background-color: #0056b3;
            }
            
            .danger-button {
                -fx-background-color: #dc3545;
                -fx-text-fill: white;
            }
            
            .danger-button:hover {
                -fx-background-color: #c82333;
            }
            
            .secondary-button {
                -fx-background-color: #6c757d;
                -fx-text-fill: white;
            }
            
            .secondary-button:hover {
                -fx-background-color: #545b62;
            }
            
            .info-title {
                -fx-font-size: 14px;
                -fx-font-weight: bold;
                -fx-text-fill: #495057;
            }
            
            .info-text {
                -fx-font-size: 12px;
                -fx-text-fill: #6c757d;
            }
            """;

        gameScene.getRoot().setStyle(css);
        gameRoot.getStyleClass().add("game-root");
    }

    /**
     * Starts the clock timer.
     */
    private void startClockTimer() {
        if (clockTimer != null) {
            clockTimer.stop();
        }

        clockTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (controller.isGameOver()) {
                clockTimer.stop();
                return;
            }

            PieceColor currentTurn = controller.getCurrentTurn();

            if (currentTurn == PieceColor.WHITE) {
                whiteClock.decr();
                updateClockLabels();

                if (whiteClock.outOfTime()) {
                    clockTimer.stop();
                    showGameOver("Black wins on time!");
                }
            } else {
                blackClock.decr();
                updateClockLabels();

                if (blackClock.outOfTime()) {
                    clockTimer.stop();
                    showGameOver("White wins on time!");
                }
            }
        }));
        clockTimer.setCycleCount(Timeline.INDEFINITE);
        clockTimer.play();
    }

    /**
     * Updates the clock labels display.
     */
    private void updateClockLabels() {
        if (whiteClockLabel != null && blackClockLabel != null) {
            Platform.runLater(() -> {
                whiteClockLabel.setText("White: " + whiteClock.getTime());
                blackClockLabel.setText("Black: " + blackClock.getTime());
            });
        }
    }

    /**
     * Creates the side panel with game controls and status information.
     */
    private void createSidePanel() {
        sidePanel = new VBox();
        sidePanel.getStyleClass().add("side-panel");
        GridPane.setHalignment(sidePanel, HPos.CENTER);
        GridPane.setValignment(sidePanel, VPos.TOP);

        // Game info panel
        createGameInfoPanel();

        // Clock panel
        VBox clockPanel = createClockPanel();

        // Status label
        statusLabel = new Label("Ready to start");
        statusLabel.getStyleClass().add("status-label");
        GridPane.setHalignment(statusLabel, HPos.CENTER);
        GridPane.setValignment(statusLabel, VPos.CENTER);

        // Game control buttons
        createControlButtons();

        // Add components to side panel
        sidePanel.getChildren().addAll(
                gameInfoPanel,
                clockPanel,
                statusLabel,
                newGameButton,
                surrenderButton,
                disconnectButton
        );
    }

    /**
     * Creates the game information panel.
     */
    private void createGameInfoPanel() {
        gameInfoPanel = new VBox();
        gameInfoPanel.getStyleClass().add("info-panel");
        GridPane.setHalignment(gameInfoPanel, HPos.CENTER);
        GridPane.setValignment(gameInfoPanel, VPos.CENTER);

        Label titleLabel = new Label("Game Information");
        titleLabel.getStyleClass().add("info-title");

        gameTypeLabel = new Label("Game Type: Local Game");
        gameTypeLabel.getStyleClass().add("info-text");

        playerInfoLabel = new Label("Mode: Player");
        playerInfoLabel.getStyleClass().add("info-text");

        gameInfoPanel.getChildren().addAll(titleLabel, gameTypeLabel, playerInfoLabel);
    }

    /**
     * Creates the clock panel.
     */
    private VBox createClockPanel() {
        VBox clockPanel = new VBox();
        clockPanel.getStyleClass().add("clock-panel");
        GridPane.setHalignment(clockPanel, HPos.CENTER);
        GridPane.setValignment(clockPanel, VPos.CENTER);

        Label titleLabel = new Label("Game Clock");
        titleLabel.getStyleClass().add("info-title");

        whiteClockLabel = new Label("White: 00:00:00");
        blackClockLabel = new Label("Black: 00:00:00");

        whiteClockLabel.getStyleClass().addAll("clock-label", "white-clock");
        blackClockLabel.getStyleClass().addAll("clock-label", "black-clock");

        clockPanel.getChildren().addAll(titleLabel, whiteClockLabel, blackClockLabel);

        return clockPanel;
    }

    /**
     * Creates the control buttons.
     */
    private void createControlButtons() {
        newGameButton = new Button("New Game");
        newGameButton.getStyleClass().addAll("game-button", "primary-button");
        newGameButton.setOnAction(e -> showStartMenu());

        surrenderButton = new Button("Surrender");
        surrenderButton.getStyleClass().addAll("game-button", "danger-button");
        surrenderButton.setOnAction(e -> {
            if (isNetworkGame && clientMode == ClientMode.SPECTATOR) {
                showAlert("Action Not Allowed", "Spectators cannot surrender!", Alert.AlertType.WARNING);
                return;
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Surrender");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Are you sure you want to surrender?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    controller.surrender();
                    PieceColor currentPlayer = controller.getCurrentTurn();
                    PieceColor winner = (currentPlayer == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
                    String message = winner.toString() + " wins by surrender";
                    showGameOver(message);
                }
            });
        });

        disconnectButton = new Button("Disconnect");
        disconnectButton.getStyleClass().addAll("game-button", "secondary-button");
        disconnectButton.setVisible(false); // Hidden for local games
        disconnectButton.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm Disconnect");
            confirmAlert.setHeaderText(null);
            confirmAlert.setContentText("Are you sure you want to disconnect?");

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    controller.disconnect();
                    showStartMenu();
                }
            });
        });
    }

    /**
     * Shows the start menu and hides the game board.
     */
    public void showStartMenu() {
        if (clockTimer != null) {
            clockTimer.stop();
        }

        Platform.runLater(() -> {
            startMenu.show();
        });
    }

    /**
     * Starts a new local game with the specified parameters.
     */
    public void startLocalGame(String gameMode, int h, int m, int s) {
        this.isNetworkGame = false;
        this.clientMode = ClientMode.PLAYER;
        this.gameMode = gameMode;

        Platform.runLater(() -> {
            setupGameUI();

            // Initialize clocks and start game
            initializeClocks(h, m, s);
            controller.startNewGame(gameMode);

            // Update UI for local game
            chessBoardUI.setNetworkGame(false);
            chessBoardUI.setClientMode(ClientMode.PLAYER);

            updateGameInfo();
            updateStatus("White's turn");

            // Hide disconnect button for local games
            disconnectButton.setVisible(false);
        });
    }

    /**
     * Starts a network game with the specified parameters.
     */
    public void startNetworkGame(ClientMode mode, PieceColor color, String serverHost, int serverPort) {
        this.isNetworkGame = true;
        this.clientMode = mode;
        this.playerColor = color;
        this.gameMode = "Network Game";

        Platform.runLater(() -> {
            setupGameUI();

            // Connect to server
            boolean connected = controller.connectToServer(serverHost, serverPort);
            if (!connected) {
                showAlert("Connection Error", "Failed to connect to server!", Alert.AlertType.ERROR);
                showStartMenu();
                return;
            }

            // Initialize clocks (will be managed by server)
            initializeClocks(0, 10, 0); // Default 10 minutes

            // Update UI for network game
            chessBoardUI.setNetworkGame(true);
            chessBoardUI.setClientMode(mode);
            chessBoardUI.setPlayerColor(color);

            updateGameInfo();
            updateStatus(mode == ClientMode.SPECTATOR ? "Spectating game" : "Waiting for opponent...");

            // Show disconnect button for network games
            disconnectButton.setVisible(true);
        });
    }

    /**
     * Sets up the main game UI layout.
     */
    private void setupGameUI() {
        primaryStage.setScene(gameScene);
        primaryStage.setTitle("Chess Game - " + gameMode);
    }

    /**
     * Updates the game information panel.
     */
    private void updateGameInfo() {
        Platform.runLater(() -> {
            gameTypeLabel.setText("Game Type: " + gameMode);

            String playerInfo = "Mode: " + (clientMode == ClientMode.SPECTATOR ? "Spectator" : "Player");
            if (isNetworkGame && clientMode == ClientMode.PLAYER) {
                playerInfo += " (" + playerColor + ")";
            }
            playerInfoLabel.setText(playerInfo);
        });
    }

    /**
     * Updates the status label with the current game state.
     */
    public void updateStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    /**
     * Shows a game over dialog with the specified message.
     */
    public void showGameOver(String message) {
        Platform.runLater(() -> {
            if (clockTimer != null) {
                clockTimer.stop();
            }
            showAlert("Game Over", message, Alert.AlertType.INFORMATION);
        });
    }

    /**
     * Shows an alert dialog with the specified parameters.
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Refreshes the chess board UI to reflect the current game state.
     */
    public void refreshBoard() {
        Platform.runLater(() -> {
            if (chessBoardUI != null) {
                chessBoardUI.updateBoard();
            }
        });
    }

    /**
     * Updates the game clocks display.
     */
    public void updateClocks(String whiteTime, String blackTime) {
        Platform.runLater(() -> {
            if (whiteClockLabel != null) {
                whiteClockLabel.setText("White: " + whiteTime);
            }
            if (blackClockLabel != null) {
                blackClockLabel.setText("Black: " + blackTime);
            }
        });
    }

    /**
     * Handles network connection lost.
     */
    public void onConnectionLost() {
        Platform.runLater(() -> {
            if (clockTimer != null) {
                clockTimer.stop();
            }
            showAlert("Connection Error", "Connection to server lost!", Alert.AlertType.ERROR);
            showStartMenu();
        });
    }

    /**
     * Returns the ChessBoardUI component.
     */
    public ChessBoardUI getChessBoardUI() {
        return chessBoardUI;
    }

    /**
     * Sets the client mode and updates UI accordingly.
     */
    public void setClientMode(ClientMode mode) {
        this.clientMode = mode;
        if (chessBoardUI != null) {
            chessBoardUI.setClientMode(mode);
        }
        updateGameInfo();
    }

    /**
     * Sets the player color and updates UI accordingly.
     */
    public void setPlayerColor(PieceColor color) {
        this.playerColor = color;
        if (chessBoardUI != null) {
            chessBoardUI.setPlayerColor(color);
        }
        updateGameInfo();
    }

    /**
     * Gets the primary stage.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }
}