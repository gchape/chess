package io.gchape.github.view;

import io.gchape.github.controller.ChessController;
import io.gchape.github.controller.client.ClientController;
import io.gchape.github.controller.server.ServerController;
import io.gchape.github.model.Clock;
import io.gchape.github.model.ServerModel;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * JavaFX-based Start Menu that allows players to configure and start chess games.
 * Supports both local games and network-based multiplayer games.
 */
public class StartMenu {
    private final Stage primaryStage;
    private ChessController chessController;
    private ClientController clientController;
    private ServerController serverController;
    private ClientView clientView;

    // UI Components
    private ComboBox<String> gameModeSelector;
    private ComboBox<String> hoursComboBox;
    private ComboBox<String> minutesComboBox;
    private ComboBox<String> secondsComboBox;

    // Network components
    private TextField serverHostField;
    private TextField serverPortField;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField emailField;
    private Button loginButton;
    private Button registerButton;
    private Button hostServerButton;
    private Button joinGameButton;
    private Button spectateButton;
    private Button connectButton;
    private Button disconnectButton;
    private Label connectionStatusLabel;
    private Button startLocalButton;

    // Panels for different modes
    private VBox networkPanel;
    private VBox localPanel;
    private TabPane contentTabs;

    // Scene
    private Scene startMenuScene;

    // Server hosting
    private boolean isHostingServer = false;

    // Callback for starting games
    private GameStartCallback gameStartCallback;

    /**
     * Interface for game start callbacks
     */
    public interface GameStartCallback {
        void startLocalGame(String gameMode, int hours, int minutes, int seconds);
        void startNetworkGame();
    }

    /**
     * Constructs a new StartMenu within the primary stage.
     */
    public StartMenu(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize controllers
        this.chessController = new ChessController();
        this.clientController = new ClientController();

        // Initialize server components - Create ServerController without ServerView parameter
        ServerModel serverModel = new ServerModel();
        this.serverController = new ServerController();//ServerView.INSTANCE, serverModel)

        // Create ClientView for network games
        this.clientView = new ClientView(primaryStage);
        this.clientView.setClientController(clientController);
        this.clientView.setChessController(chessController);
        this.clientController.setClientView(clientView);

        // Initialize components
        initializeComponents();
        createScene();
        setupEventHandlers();
    }

    /**
     * Sets the callback for game start events
     */
    public void setGameStartCallback(GameStartCallback callback) {
        this.gameStartCallback = callback;
    }

    /**
     * Initialize all UI components.
     */
    private void initializeComponents() {
        // Create tabs for different game types
        contentTabs = new TabPane();
        contentTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create local and network game panels
        localPanel = createLocalGamePanel();
        networkPanel = createNetworkGamePanel();

        // Create tabs
        Tab localTab = new Tab("Local Game", localPanel);
        Tab networkTab = new Tab("Network Game", networkPanel);

        contentTabs.getTabs().addAll(localTab, networkTab);
    }

    /**
     * Creates the main scene.
     */
    private void createScene() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        GridPane.setHalignment(root, HPos.CENTER);
        GridPane.setValignment(root, VPos.CENTER);
        root.getStyleClass().add("start-menu-root");

        // Title
        Label titleLabel = new Label("Chess Game");
        titleLabel.getStyleClass().add("title-label");

        // Subtitle
        Label subtitleLabel = new Label("Choose your game mode");
        subtitleLabel.getStyleClass().add("subtitle-label");

        // Content
        contentTabs.setPrefWidth(650);
        contentTabs.setPrefHeight(550);

        root.getChildren().addAll(titleLabel, subtitleLabel, contentTabs);

        startMenuScene = new Scene(root, 750, 650);

        // Add CSS styling
        String css = """
            .start-menu-root {
                -fx-background-color: linear-gradient(to bottom, #f0f0f0, #e0e0e0);
            }
            
            .title-label {
                -fx-font-size: 36px;
                -fx-font-weight: bold;
                -fx-text-fill: #2c3e50;
            }
            
            .subtitle-label {
                -fx-font-size: 16px;
                -fx-text-fill: #666666;
            }
            
            .section-title {
                -fx-font-weight: bold;
                -fx-font-size: 14px;
                -fx-text-fill: #34495e;
            }
            
            .game-button {
                -fx-font-size: 14px;
                -fx-padding: 10 20 10 20;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
            }
            
            .start-button {
                -fx-background-color: #27ae60;
                -fx-text-fill: white;
            }
            
            .start-button:hover {
                -fx-background-color: #2ecc71;
            }
            
            .network-button {
                -fx-background-color: #3498db;
                -fx-text-fill: white;
            }
            
            .network-button:hover {
                -fx-background-color: #5dade2;
            }
            
            .host-button {
                -fx-background-color: #e74c3c;
                -fx-text-fill: white;
            }
            
            .host-button:hover {
                -fx-background-color: #ec7063;
            }
            
            .section-panel {
                -fx-border-color: #bdc3c7;
                -fx-border-width: 1;
                -fx-padding: 15;
                -fx-background-color: white;
                -fx-border-radius: 5;
                -fx-background-radius: 5;
            }
            """;

        startMenuScene.getRoot().setStyle(css);
    }

    /**
     * Creates the local game panel.
     */
    private VBox createLocalGamePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        GridPane.setHalignment(panel, HPos.CENTER);
        GridPane.setValignment(panel, VPos.CENTER);

        // Game mode selector
        VBox gameModePanel = new VBox(10);
        GridPane.setHalignment(gameModePanel, HPos.CENTER);
        GridPane.setValignment(gameModePanel, VPos.CENTER);

        Label gameModeLabel = new Label("Select Game Mode:");
        gameModeLabel.getStyleClass().add("section-title");

        gameModeSelector = new ComboBox<>();
        gameModeSelector.getItems().addAll("Player vs Player", "Player vs Computer", "Computer vs Computer");
        gameModeSelector.setValue("Player vs Player");
        gameModeSelector.setPrefWidth(250);

        gameModePanel.getChildren().addAll(gameModeLabel, gameModeSelector);

        // Clock settings
        VBox clockPanel = createClockPanel();

        // Start button for local games
        startLocalButton = new Button("Start Local Game");
        startLocalButton.setPrefWidth(200);
        startLocalButton.getStyleClass().addAll("game-button", "start-button");

        panel.getChildren().addAll(gameModePanel, clockPanel, startLocalButton);

        return panel;
    }

    /**
     * Creates the clock configuration panel.
     */
    private VBox createClockPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("section-panel");
        GridPane.setHalignment(panel, HPos.CENTER);
        GridPane.setValignment(panel, VPos.CENTER);

        Label titleLabel = new Label("Game Clock Settings");
        titleLabel.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        GridPane.setHalignment(grid, HPos.CENTER);
        GridPane.setValignment(grid, VPos.CENTER);

        // Hours
        grid.add(new Label("Hours:"), 0, 0);
        hoursComboBox = new ComboBox<>();
        for (int i = 0; i <= 23; i++) {
            hoursComboBox.getItems().add(String.format("%02d", i));
        }
        hoursComboBox.setValue("01");
        hoursComboBox.setPrefWidth(80);
        grid.add(hoursComboBox, 1, 0);

        // Minutes
        grid.add(new Label("Minutes:"), 2, 0);
        minutesComboBox = new ComboBox<>();
        for (int i = 0; i <= 59; i++) {
            minutesComboBox.getItems().add(String.format("%02d", i));
        }
        minutesComboBox.setValue("30");
        minutesComboBox.setPrefWidth(80);
        grid.add(minutesComboBox, 3, 0);

        // Seconds
        grid.add(new Label("Seconds:"), 4, 0);
        secondsComboBox = new ComboBox<>();
        for (int i = 0; i <= 59; i++) {
            secondsComboBox.getItems().add(String.format("%02d", i));
        }
        secondsComboBox.setValue("00");
        secondsComboBox.setPrefWidth(80);
        grid.add(secondsComboBox, 5, 0);

        // Clock preview
        Label previewLabel = new Label();
        previewLabel.textProperty().bind(Bindings.concat(
                "Clock: ", hoursComboBox.valueProperty(), ":",
                minutesComboBox.valueProperty(), ":",
                secondsComboBox.valueProperty()
        ));
        previewLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 14px;");

        panel.getChildren().addAll(titleLabel, grid, previewLabel);

        return panel;
    }

    /**
     * Creates the network game panel.
     */
    private VBox createNetworkGamePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(20));
        GridPane.setHalignment(panel, HPos.CENTER);
        GridPane.setValignment(panel, VPos.CENTER);

        // Connection settings
        VBox connectionPanel = createConnectionPanel();

        // Authentication panel
        VBox authPanel = createAuthenticationPanel();

        // Game actions panel
        VBox actionsPanel = createGameActionsPanel();

        // Status label
        connectionStatusLabel = new Label("Not connected");
        connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");

        panel.getChildren().addAll(connectionPanel, authPanel, actionsPanel, connectionStatusLabel);

        return panel;
    }

    /**
     * Creates the connection settings panel.
     */
    private VBox createConnectionPanel() {
        VBox panel = new VBox(15);
        panel.getStyleClass().add("section-panel");

        Label titleLabel = new Label("Server Connection");
        titleLabel.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        GridPane.setHalignment(grid, HPos.CENTER);
        GridPane.setValignment(grid, VPos.CENTER);

        // Server host
        grid.add(new Label("Server Host:"), 0, 0);
        serverHostField = new TextField("localhost");
        serverHostField.setPrefWidth(200);
        grid.add(serverHostField, 1, 0);

        // Server port
        grid.add(new Label("Server Port:"), 0, 1);
        serverPortField = new TextField("8080");
        serverPortField.setPrefWidth(200);
        grid.add(serverPortField, 1, 1);

        // Buttons
        hostServerButton = new Button("Host Server");
        hostServerButton.setPrefWidth(120);
        hostServerButton.getStyleClass().addAll("game-button", "host-button");

        connectButton = new Button("Connect");
        connectButton.setPrefWidth(120);
        connectButton.getStyleClass().addAll("game-button", "network-button");

        disconnectButton = new Button("Disconnect");
        disconnectButton.setPrefWidth(120);
        disconnectButton.getStyleClass().addAll("game-button", "network-button");
        disconnectButton.setDisable(true);

        HBox buttonBox = new HBox(10);
        GridPane.setHalignment(buttonBox, HPos.CENTER);
        GridPane.setValignment(buttonBox, VPos.CENTER);
        buttonBox.getChildren().addAll(hostServerButton, connectButton, disconnectButton);

        panel.getChildren().addAll(titleLabel, grid, buttonBox);

        return panel;
    }

    /**
     * Creates the authentication panel.
     */
    private VBox createAuthenticationPanel() {
        VBox panel = new VBox(15);
        panel.getStyleClass().add("section-panel");

        Label titleLabel = new Label("Player Authentication");
        titleLabel.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        GridPane.setHalignment(grid, HPos.CENTER);
        GridPane.setValignment(grid, VPos.CENTER);

        // Username
        grid.add(new Label("Username:"), 0, 0);
        usernameField = new TextField();
        usernameField.setPrefWidth(200);
        usernameField.setPromptText("Enter username");
        grid.add(usernameField, 1, 0);

        // Password
        grid.add(new Label("Password:"), 0, 1);
        passwordField = new PasswordField();
        passwordField.setPrefWidth(200);
        passwordField.setPromptText("Enter password");
        grid.add(passwordField, 1, 1);

        // Email (for registration)
        grid.add(new Label("Email:"), 0, 2);
        emailField = new TextField();
        emailField.setPrefWidth(200);
        emailField.setPromptText("For registration only");
        grid.add(emailField, 1, 2);

        // Buttons
        loginButton = new Button("Login");
        loginButton.setPrefWidth(100);
        loginButton.getStyleClass().addAll("game-button", "start-button");
        loginButton.setDisable(true);

        registerButton = new Button("Register");
        registerButton.setPrefWidth(100);
        registerButton.getStyleClass().addAll("game-button", "network-button");
        registerButton.setDisable(true);

        HBox authButtonBox = new HBox(10);
        GridPane.setHalignment(authButtonBox, HPos.CENTER);
        GridPane.setValignment(authButtonBox, VPos.CENTER);
        authButtonBox.getChildren().addAll(loginButton, registerButton);

        panel.getChildren().addAll(titleLabel, grid, authButtonBox);

        return panel;
    }

    /**
     * Creates the game actions panel.
     */
    private VBox createGameActionsPanel() {
        VBox panel = new VBox(15);
        panel.getStyleClass().add("section-panel");

        Label titleLabel = new Label("Game Actions");
        titleLabel.getStyleClass().add("section-title");

        joinGameButton = new Button("Join Game");
        joinGameButton.setPrefWidth(150);
        joinGameButton.getStyleClass().addAll("game-button", "start-button");
        joinGameButton.setDisable(true);

        spectateButton = new Button("Spectate Game");
        spectateButton.setPrefWidth(150);
        spectateButton.getStyleClass().addAll("game-button", "network-button");
        spectateButton.setDisable(true);

        HBox actionButtonBox = new HBox(10);
        GridPane.setHalignment(actionButtonBox, HPos.CENTER);
        GridPane.setValignment(actionButtonBox, VPos.CENTER);
        actionButtonBox.getChildren().addAll(joinGameButton, spectateButton);

        panel.getChildren().addAll(titleLabel, actionButtonBox);

        return panel;
    }

    /**
     * Setup event handlers for all buttons and components.
     */
    private void setupEventHandlers() {
        // Local game start button
        startLocalButton.setOnAction(e -> startLocalGame());

        // Network connection buttons
        hostServerButton.setOnAction(e -> hostServer());
        connectButton.setOnAction(e -> connectToServer());
        disconnectButton.setOnAction(e -> disconnectFromServer());

        // Authentication buttons
        loginButton.setOnAction(e -> loginUser());
        registerButton.setOnAction(e -> registerUser());

        // Game action buttons
        joinGameButton.setOnAction(e -> joinGame());
        spectateButton.setOnAction(e -> spectateGame());

        // Enable/disable buttons based on connection status
        connectButton.disableProperty().bind(
                serverHostField.textProperty().isEmpty()
                        .or(serverPortField.textProperty().isEmpty())
        );

        loginButton.disableProperty().bind(
                usernameField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );

        registerButton.disableProperty().bind(
                usernameField.textProperty().isEmpty()
                        .or(passwordField.textProperty().isEmpty())
        );
    }

    /**
     * Starts a local chess game.
     */
    private void startLocalGame() {
        try {
            String gameMode = gameModeSelector.getValue();

            // Get clock settings
            int hours = Integer.parseInt(hoursComboBox.getValue());
            int minutes = Integer.parseInt(minutesComboBox.getValue());
            int seconds = Integer.parseInt(secondsComboBox.getValue());

            // Use callback to start game if available
            if (gameStartCallback != null) {
                gameStartCallback.startLocalGame(gameMode, hours, minutes, seconds);
            } else {
                // Fallback: Create and show game window directly
                GameWindow gameWindow = new GameWindow(chessController);
                gameWindow.startLocalGame(gameMode, hours, minutes, seconds);
                primaryStage.hide(); // Hide start menu
            }

        } catch (Exception e) {
            showError("Failed to start local game", e.getMessage());
        }
    }

    /**
     * Hosts a server for multiplayer games.
     */
    private void hostServer() {
        if (isHostingServer) {
            stopHostingServer();
            return;
        }

        Task<Void> hostTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String host = serverHostField.getText().trim();
                int port = Integer.parseInt(serverPortField.getText().trim());

                Platform.runLater(() -> {
                    connectionStatusLabel.setText("Starting server...");
                    connectionStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    hostServerButton.setText("Stop Server");
                });

                serverController.startServer(host, port);

                Platform.runLater(() -> {
                    connectionStatusLabel.setText("Server running on " + host + ":" + port);
                    connectionStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    isHostingServer = true;
                    enableAuthenticationButtons(true);
                });

                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showError("Server Error", "Failed to start server: " + getException().getMessage());
                    connectionStatusLabel.setText("Failed to start server");
                    connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    hostServerButton.setText("Host Server");
                });
            }
        };

        Thread hostThread = new Thread(hostTask);
        hostThread.setDaemon(true);
        hostThread.start();
    }

    /**
     * Stops hosting the server.
     */
    private void stopHostingServer() {
        try {
            serverController.stopServer();
            isHostingServer = false;
            hostServerButton.setText("Host Server");
            connectionStatusLabel.setText("Server stopped");
            connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            enableAuthenticationButtons(false);
        } catch (Exception e) {
            showError("Server Error", "Failed to stop server: " + e.getMessage());
        }
    }

    /**
     * Connects to a remote server.
     */
    private void connectToServer() {
        Task<Void> connectTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String host = serverHostField.getText().trim();
                int port = Integer.parseInt(serverPortField.getText().trim());

                Platform.runLater(() -> {
                    connectionStatusLabel.setText("Connecting...");
                    connectionStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    connectButton.setDisable(true);
                });

                boolean connected = clientController.startClient(host, port);

                Platform.runLater(() -> {
                    if (connected) {
                        connectionStatusLabel.setText("Connected to " + host + ":" + port);
                        connectionStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                        connectButton.setDisable(true);
                        disconnectButton.setDisable(false);
                        enableAuthenticationButtons(true);
                    } else {
                        connectionStatusLabel.setText("Connection failed");
                        connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                        connectButton.setDisable(false);
                    }
                });

                return null;
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showError("Connection Error", "Failed to connect: " + getException().getMessage());
                    connectionStatusLabel.setText("Connection failed");
                    connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    connectButton.setDisable(false);
                });
            }
        };

        Thread connectThread = new Thread(connectTask);
        connectThread.setDaemon(true);
        connectThread.start();
    }

    /**
     * Disconnects from the server.
     */
    private void disconnectFromServer() {
        clientController.disconnect();
        connectionStatusLabel.setText("Disconnected");
        connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        connectButton.setDisable(false);
        disconnectButton.setDisable(true);
        enableAuthenticationButtons(false);
        enableGameButtons(false);
    }

    /**
     * Attempts to login with provided credentials.
     */
    private void loginUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (clientController.loginUser(username, password)) {
            connectionStatusLabel.setText("Logging in...");
            connectionStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        }
    }

    /**
     * Attempts to register a new user.
     */
    private void registerUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            email = username + "@example.com";
        }

        if (clientController.registerUser(username, password, email)) {
            connectionStatusLabel.setText("Registering...");
            connectionStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        }
    }

    /**
     * Joins a multiplayer game.
     */
    private void joinGame() {
        if (clientController.joinGame()) {
            connectionStatusLabel.setText("Joining game...");
            connectionStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");

            if (gameStartCallback != null) {
                gameStartCallback.startNetworkGame();
            }
        }
    }

    /**
     * Joins a game as spectator.
     */
    private void spectateGame() {
        if (clientController.spectateGame()) {
            connectionStatusLabel.setText("Joining as spectator...");
            connectionStatusLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        }
    }

    /**
     * Shows the start menu scene.
     */
    public void show() {
        primaryStage.setScene(startMenuScene);
        primaryStage.setTitle("Chess Game - Start Menu");
        primaryStage.show();
    }

    /**
     * Enables or disables authentication buttons.
     */
    private void enableAuthenticationButtons(boolean enable) {
        // Note: Individual buttons are also bound to field properties
        // This method can be used for additional state management
        if (!enable) {
            enableGameButtons(false);
        }
    }

    /**
     * Enables or disables game action buttons.
     */
    private void enableGameButtons(boolean enable) {
        joinGameButton.setDisable(!enable);
        spectateButton.setDisable(!enable);
    }

    /**
     * Shows an error dialog.
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Gets the primary stage.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Gets the chess controller.
     */
    public ChessController getChessController() {
        return chessController;
    }

    /**
     * Gets the client controller.
     */
    public ClientController getClientController() {
        return clientController;
    }

    /**
     * Called when authentication is successful
     */
    public void onAuthenticationSuccess() {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("Logged in successfully!");
            connectionStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            enableGameButtons(true);
        });
    }

    /**
     * Called when authentication fails
     */
    public void onAuthenticationFailure(String message) {
        Platform.runLater(() -> {
            connectionStatusLabel.setText("Login failed: " + message);
            connectionStatusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            enableGameButtons(false);
        });
    }
}