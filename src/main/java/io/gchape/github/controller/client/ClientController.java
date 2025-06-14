package io.gchape.github.controller.client;

import io.gchape.github.view.ClientView;
import io.gchape.github.model.message.*;
import io.gchape.github.model.entity.ClientMode;
import javafx.application.Platform;
import java.io.IOException;

public class ClientController {
    private ClientView clientView;
    private Client client;
    private boolean isConnected = false;
    private boolean isLoggedIn = false;
    private Long currentUserId;
    private String currentUsername;
    private ClientMode clientMode = ClientMode.PLAYER;

    public ClientController() {
        // Default constructor
    }

    public ClientController(ClientView clientView) {
        this.clientView = clientView;
        this.clientView.setClientController(this);
    }

    public void setClientView(ClientView clientView) {
        this.clientView = clientView;
        this.clientView.setClientController(this);
    }

    /**
     * Start client connection to server
     */
    public boolean startClient(final String host, final int port) {
        try {
            client = new Client(host, port, this);
            isConnected = true;

            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Connected to server", false));
            }
            return true;
        } catch (Exception e) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Failed to connect: " + e.getMessage(), true));
            }
            return false;
        }
    }

    /**
     * Check if client is connected to server
     */
    public boolean isConnected() {
        return isConnected && client != null && client.isConnected();
    }

    /**
     * Close client connection
     */
    public void closeClient() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        } finally {
            isConnected = false;
            isLoggedIn = false;
            currentUserId = null;
            currentUsername = null;
        }
    }

    /**
     * Handle user login request - method expected by ClientView
     */
    public void handleLogin(String username, String password) {
        loginUser(username, password);
    }

    /**
     * Handle user registration request - method expected by ClientView
     */
    public void handleRegister(String username, String password, String email) {
        registerUser(username, password, email);
    }

    /**
     * Handle join game request - method expected by ClientView
     */
    public void handleJoinGame() {
        joinGame();
    }

    /**
     * Handle disconnect request - method expected by ClientView
     */
    public void handleDisconnect() {
        disconnect();
    }

    /**
     * Handle user login request
     */
    public boolean loginUser(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Please enter both username and password", true));
            }
            return false;
        }

        if (!isConnected()) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Not connected to server", true));
            }
            return false;
        }

        try {
            LoginMessage loginMsg = new LoginMessage(username.trim(), password);
            this.currentUsername = username.trim();

            client.send(loginMsg.toJson());

            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Logging in...", false));
            }
            return true;
        } catch (Exception e) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Login failed: " + e.getMessage(), true));
            }
            return false;
        }
    }

    /**
     * Handle user registration request with email
     */
    public boolean registerUser(String username, String password, String email) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Please enter username and password", true));
            }
            return false;
        }

        if (!isConnected()) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Not connected to server", true));
            }
            return false;
        }

        try {
            String finalEmail = (email != null && !email.trim().isEmpty()) ?
                    email.trim() : username.trim() + "@example.com";

            RegisterMessage regMsg = new RegisterMessage(username.trim(), password, finalEmail);
            this.currentUsername = username.trim();

            client.send(regMsg.toJson());

            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Registering...", false));
            }
            return true;
        } catch (Exception e) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Registration failed: " + e.getMessage(), true));
            }
            return false;
        }
    }

    /**
     * Handle join game request
     */
    public boolean joinGame() {
        if (!isLoggedIn) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Please login first", true));
            }
            return false;
        }

        if (!isConnected()) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Not connected to server", true));
            }
            return false;
        }

        try {
            JoinGameMessage joinMsg = new JoinGameMessage();
            joinMsg.setGameMode("STANDARD");
            joinMsg.setUserId(currentUserId);

            client.send(joinMsg.toJson());

            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Joining game...", false));
            }
            return true;
        } catch (Exception e) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Failed to join game: " + e.getMessage(), true));
            }
            return false;
        }
    }

    /**
     * Handle spectate game request
     */
    public boolean spectateGame() {
        if (!isLoggedIn) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Please login first", true));
            }
            return false;
        }

        if (!isConnected()) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Not connected to server", true));
            }
            return false;
        }

        try {
            SpectateMessage spectateMsg = new SpectateMessage();
            spectateMsg.setUserId(currentUserId);

            client.send(spectateMsg.toJson());
            clientMode = ClientMode.SPECTATOR;

            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Joining as spectator...", false));
            }
            return true;
        } catch (Exception e) {
            if (clientView != null) {
                Platform.runLater(() ->
                        clientView.updateLoginStatus("Failed to spectate: " + e.getMessage(), true));
            }
            return false;
        }
    }

    /**
     * Handle disconnect request
     */
    public void disconnect() {
        try {
            if (isConnected() && client != null) {
                DisconnectionMessage disconnectMsg = new DisconnectionMessage();
                disconnectMsg.setUserId(currentUserId);
                client.send(disconnectMsg.toJson());
            }
        } catch (Exception e) {
            System.err.println("Error sending disconnect message: " + e.getMessage());
        } finally {
            closeClient();
            if (clientView != null) {
                Platform.runLater(() -> {
                    clientView.updateLoginStatus("Disconnected", false);
                    clientView.setJoinGameEnabled(false);
                    clientView.showLoginPanel();
                });
            }
        }
    }

    /**
     * Send move to server
     */
    public void sendMove(String fromSquare, String toSquare, String promotion) {
        if (!isLoggedIn || !isConnected()) {
            return;
        }

        try {
            MoveMessage moveMsg = new MoveMessage();
            moveMsg.setFromSquare(fromSquare);
            moveMsg.setToSquare(toSquare);
            moveMsg.setPromotion(promotion);
            moveMsg.setUserId(currentUserId);

            client.send(moveMsg.toJson());
        } catch (Exception e) {
            System.err.println("Failed to send move: " + e.getMessage());
        }
    }

    /**
     * Send surrender to server
     */
    public void sendSurrender() {
        if (!isLoggedIn || !isConnected()) {
            return;
        }

        try {
            SurrenderMessage surrenderMsg = new SurrenderMessage();
            surrenderMsg.setUserId(currentUserId);

            client.send(surrenderMsg.toJson());
        } catch (Exception e) {
            System.err.println("Failed to send surrender: " + e.getMessage());
        }
    }

    // Message handling methods called by Client
    public void handleMessage(String messageJson) {
        System.out.println("Handling plain text message: " + messageJson);
        // Handle plain text messages if needed
    }

    /**
     * Handle successful authentication response
     */
    public void handleAuthSuccess() {
        isLoggedIn = true;
        if (clientView != null) {
            Platform.runLater(() -> {
                clientView.updateLoginStatus("Successfully logged in as " + currentUsername + "!", false);
                clientView.setJoinGameEnabled(true);

                // Auto-join game or show game options
                // For now, just enable the join game button
                System.out.println("Authentication successful for user: " + currentUsername + " (ID: " + currentUserId + ")");
            });
        }
    }

    /**
     * Handle authentication failure
     */
    public void handleAuthFailure(String message) {
        isLoggedIn = false;
        currentUserId = null;
        if (clientView != null) {
            Platform.runLater(() -> {
                clientView.updateLoginStatus("Authentication failed: " + message, true);
                clientView.setJoinGameEnabled(false);
            });
        }
    }

    /**
     * Handle game joined successfully
     */
    public void handleGameJoined() {
        if (clientView != null) {
            Platform.runLater(() -> {
                clientView.showGameBoard();
                clientView.updateGameStatus("Game joined successfully! Waiting for opponent...");
            });
        }
    }

    /**
     * Handle move received from server
     */
    public void handleMoveReceived(String moveJson) {
        // Parse and forward to chess controller
        if (clientView != null) {
            Platform.runLater(() -> {
                // Update board through chess controller
                // This would need proper message parsing
                System.out.println("Move received: " + moveJson);
            });
        }
    }

    /**
     * Handle game state update from server
     */
    public void handleGameStateUpdate(String gameStateJson) {
        // Parse and update local game state
        if (clientView != null) {
            Platform.runLater(() -> {
                // Update UI with new game state
                System.out.println("Game state update: " + gameStateJson);
            });
        }
    }

    /**
     * Handle connection lost
     */
    public void handleConnectionLost() {
        isConnected = false;
        isLoggedIn = false;
        if (clientView != null) {
            Platform.runLater(() -> {
                clientView.updateLoginStatus("Connection lost", true);
                clientView.setJoinGameEnabled(false);
                clientView.showLoginPanel();
            });
        }
    }

    // Getters and setters
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    public ClientMode getClientMode() {
        return clientMode;
    }

    public void setClientMode(ClientMode clientMode) {
        this.clientMode = clientMode;
    }
}