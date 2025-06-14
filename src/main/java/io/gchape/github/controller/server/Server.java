package io.gchape.github.controller.server;

import io.gchape.github.model.message.*;
import io.gchape.github.service.*;
import io.gchape.github.service.GameSessionManager;
import io.gchape.github.model.entity.ClientMode;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// You'll need to create these classes if they don't exist


public class Server implements Closeable {
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    private final IntegerProperty connectedClients;
    private final StringProperty serverStatus;

    private final ByteBuffer cache;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<SocketChannel, ClientMode> connections;

    private Selector clientSelector;
    private Selector acceptSelector;
    private ServerSocketChannel server;
    private DatabaseService databaseService;
    private GameSessionManager sessionManager;
    private AuthenticationService authenticationService;
    private Map<SocketChannel, UserSession> userSessions; // Changed to UserSession
    private Map<SocketChannel, Long> clientGameSessions;

    public Server() {
        this.connections = new ConcurrentHashMap<>();
        this.cache = ByteBuffer.allocate(16 * 1024);
        this.executorService = Executors.newFixedThreadPool(2);

        this.serverStatus = new SimpleStringProperty();
        this.connectedClients = new SimpleIntegerProperty(0);
        this.databaseService = DatabaseService.getInstance();
        this.sessionManager = new GameSessionManager(databaseService);
        this.authenticationService = new AuthenticationService(databaseService);
        this.userSessions = new ConcurrentHashMap<>();
        this.clientGameSessions = new ConcurrentHashMap<>();
    }

    public IntegerProperty connectedClientsProperty() {
        return connectedClients;
    }

    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public void startServer(final String host, final int port) {
        try {
            var serverAddress = new InetSocketAddress(host, port);

            server = ServerSocketChannel.open();
            server
                    .bind(serverAddress)
                    .configureBlocking(false);

            clientSelector = Selector.open();
            acceptSelector = Selector.open();
            server.register(acceptSelector, SelectionKey.OP_ACCEPT);

            updateState("Server started at={ %s }.".formatted(serverAddress), 0);

            executorService.submit(this::watchAcceptable);
            executorService.submit(this::watchReadable);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start server", e);
        }
    }

    private void watchAcceptable() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (acceptSelector.select(1000) == 0) {
                    continue;
                }

                var keys = acceptSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        acceptNewClient((ServerSocketChannel) key.channel());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void acceptNewClient(final ServerSocketChannel acceptableChannel) throws IOException {
        Optional.ofNullable(acceptableChannel.accept())
                .ifPresent(clientChannel -> {
                    try {
                        clientChannel.configureBlocking(false);
                        clientChannel.register(clientSelector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

                        updateState("New client connected at={ %s }."
                                .formatted(LocalTime.now()), 1);

                        syncState(clientChannel);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void watchReadable() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (clientSelector.select(1000) == 0) {
                    continue;
                }

                var keys = clientSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    iterator.remove();

                    try {
                        var clientChannel = (SocketChannel) key.channel();

                        if (key.isReadable()) {
                            read(clientChannel, key);
                        }
                    } catch (IOException e) {
                        closeClient((SocketChannel) key.channel(), key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void read(final SocketChannel clientChannel,
                      final SelectionKey key) throws IOException {
        var buffer = (ByteBuffer) key.attachment();
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            closeClient(clientChannel, key);
            return;
        } else if (bytesRead == 0) {
            return;
        }

        buffer.flip();
        var message = CHARSET.decode(buffer).toString().trim();

        if (!message.isEmpty()) {
            synchronized (cache) {
                cache.put(CHARSET.encode("%s%n".formatted(message)));
            }

            updateState("Received={ %s } from client.".formatted(message), 0);

            // Check if message is JSON (structured) or plain text
            if (message.startsWith("{") && message.endsWith("}")) {
                handleStructuredMessage(clientChannel, message);
            } else {
                broadcastMessage(message, clientChannel);
            }
        }

        buffer.clear();
    }

    private void broadcastMessage(final String message, final SocketChannel sender) throws IOException {
        var broadcastBuffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));

        synchronized (connections) {
            for (SocketChannel client : connections.keySet()) {
                if (!client.equals(sender) && client.isConnected()) {
                    try {
                        broadcastBuffer.rewind();
                        client.write(broadcastBuffer);
                    } catch (IOException e) {
                        var key = client.keyFor(clientSelector);
                        if (key != null) {
                            closeClient(client, key);
                        }
                    }
                }
            }
        }
    }

    private void syncState(SocketChannel clientChannel) throws IOException {
        synchronized (cache) {
            ByteBuffer snapshot = cache.asReadOnlyBuffer().flip();

            ClientMode clientMode;
            if (connectedClients.get() > 2) {
                clientMode = ClientMode.SPECTATOR;
            } else {
                clientMode = ClientMode.PLAYER;
            }

            connections.put(clientChannel, clientMode);

            ByteBuffer mode = CHARSET.encode("%s%n".formatted(clientMode));
            ByteBuffer response = ByteBuffer.allocate(mode.remaining() + snapshot.remaining());

            response.put(mode);
            response.put(snapshot);

            response.flip();
            while (response.hasRemaining()) {
                clientChannel.write(response);
            }
        }
    }

    private void closeClient(final SocketChannel clientChannel, final SelectionKey key) throws IOException {
        key.cancel();

        var mode = connections.get(clientChannel);
        connections.remove(clientChannel);

        // Clean up user session and game session
        UserSession userSession = userSessions.remove(clientChannel);
        Long gameId = clientGameSessions.remove(clientChannel);

        // If user was in a game, handle cleanup
        if (userSession != null && gameId != null) {
            try {
                // End the game session if player disconnects
                sessionManager.endGameSession(gameId, "DISCONNECT");

                // Notify other players in the game
                broadcastGameDisconnection(gameId, userSession.getUserId());
            } catch (Exception e) {
                System.err.println("Error cleaning up game session: " + e.getMessage());
            }
        }

        clientChannel.close();

        updateState("Client disconnected at={ %s }.".formatted(LocalTime.now()), -1);

        if (mode == ClientMode.PLAYER) {
            updateState("Session closed at={ %s }".formatted(LocalTime.now()), 0);
        }
    }

    private void updateState(final String serverUpdate, final int payload) {
        serverStatus.set(serverUpdate);

        switch (payload) {
            case 1 -> connectedClients.set(connectedClients.get() + 1);
            case -1 -> connectedClients.set(connectedClients.get() - 1);
            default -> {
            }
        }
    }

    @Override
    public void close() throws IOException {
        disconnectClients();

        server.close();
        clientSelector.close();
        acceptSelector.close();
        executorService.shutdownNow();
    }

    private void disconnectClients() {
        connections.keySet()
                .parallelStream()
                .forEach(socketChannel -> {
                    try {
                        socketChannel.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        connections.clear();
        userSessions.clear();
        clientGameSessions.clear();
        connectedClients.set(0);
    }

    // Handle structured messages
    private void handleStructuredMessage(SocketChannel clientChannel, String messageJson) {
        try {
            Message message = Message.fromJson(messageJson);

//            // First parse as generic Message to get the type
//            Message message = Message.fromJson(messageJson, Message.class);
//
//            // Then parse as specific message type based on the type field
//            switch (message.getType()) {
//                case "LOGIN" -> {
//                    LoginMessage loginMsg = Message.fromJson(messageJson, LoginMessage.class);
//                    handleLogin(clientChannel, loginMsg);
//                }
//                case "REGISTER" -> {
//                    RegisterMessage regMsg = Message.fromJson(messageJson, RegisterMessage.class);
//                    handleRegister(clientChannel, regMsg);
//                }
//                case "JOIN_GAME" -> {
//                    JoinGameMessage joinMsg = Message.fromJson(messageJson, JoinGameMessage.class);
//                    handleJoinGame(clientChannel, joinMsg);
//                }
//                case "MOVE" -> {
//                    MoveMessage moveMsg = Message.fromJson(messageJson, MoveMessage.class);
//                    handleMove(clientChannel, moveMsg);
//                }
//                case "SPECTATE" -> {
//                    SpectateMessage spectateMsg = Message.fromJson(messageJson, SpectateMessage.class);
//                    handleSpectate(clientChannel, spectateMsg);
//                }
//                default -> sendError(clientChannel, "UNKNOWN_MESSAGE", "Unknown message type");
//            }
        } catch (Exception e) {
            sendError(clientChannel, "PARSE_ERROR", "Failed to parse message: " + e.getMessage());
        }
    }

    private void handleLogin(SocketChannel client, LoginMessage loginMsg) {
        try {
            System.out.println("Processing login for user: " + loginMsg.getUsername());

            // Authenticate user using AuthenticationService
            AuthResult authResult = authenticationService.authenticateUser(
                    loginMsg.getUsername(),
                    loginMsg.getPassword()
            );

            AuthResponseMessage response;

            if (authResult.isSuccess()) {
                // Create user session
                Long userId = authResult.getUserId();
                String username = loginMsg.getUsername();

                // Store user session
                userSessions.put(client, new UserSession(userId, username));

                // Send success response
                response = new AuthResponseMessage(
                        true,
                        "Login successful",
                        userId,
                        username
                );

                System.out.println("Login successful for user: " + username + " (ID: " + userId + ")");

            } else {
                // Send failure response
                response = new AuthResponseMessage(
                        false,
                        authResult.getMessage(),
                        null,
                        null
                );

                System.out.println("Login failed for user: " + loginMsg.getUsername() +
                        " - " + authResult.getMessage());
            }

            sendMessage(client, response);

        } catch (Exception e) {
            System.err.println("Error handling login: " + e.getMessage());
            e.printStackTrace();

            AuthResponseMessage errorResponse = new AuthResponseMessage(
                    false,
                    "Server error during login: " + e.getMessage(),
                    null,
                    null
            );

            sendMessage(client, errorResponse);
        }
    }

    private void handleRegister(SocketChannel client, RegisterMessage regMsg) {
        try {
            System.out.println("Processing registration for user: " + regMsg.getUsername());

            // Register user using AuthenticationService
            AuthResult authResult = authenticationService.registerUser(
                    regMsg.getUsername(),
                    regMsg.getPassword(),
                    regMsg.getEmail()
            );

            AuthResponseMessage response;

            if (authResult.isSuccess()) {
                // Create user session
                Long userId = authResult.getUserId();
                String username = regMsg.getUsername();

                // Store user session
                userSessions.put(client, new UserSession(userId, username));

                // Send success response
                response = new AuthResponseMessage(
                        true,
                        "Registration successful",
                        userId,
                        username
                );

                System.out.println("Registration successful for user: " + username + " (ID: " + userId + ")");

            } else {
                // Send failure response
                response = new AuthResponseMessage(
                        false,
                        authResult.getMessage(),
                        null,
                        null
                );

                System.out.println("Registration failed for user: " + regMsg.getUsername() +
                        " - " + authResult.getMessage());
            }

            sendMessage(client, response);

        } catch (Exception e) {
            System.err.println("Error handling registration: " + e.getMessage());
            e.printStackTrace();

            AuthResponseMessage errorResponse = new AuthResponseMessage(
                    false,
                    "Server error during registration: " + e.getMessage(),
                    null,
                    null
            );

            sendMessage(client, errorResponse);
        }
    }

    private void handleJoinGame(SocketChannel client, JoinGameMessage joinMsg) {
        UserSession userSession = userSessions.get(client);
        if (userSession == null) {
            sendError(client, "AUTH_REQUIRED", "Must be logged in to join game");
            return;
        }

        try {
            // Use the new joinOrCreateGame method instead of just creating
            Long gameId = sessionManager.joinOrCreateGame(userSession.getUserId(), joinMsg.getGameMode());
            clientGameSessions.put(client, gameId);

            // Send initial game state
            GameStateMessage gameState = sessionManager.getGameState(gameId);
            if (gameState == null) {
                // Fallback if getGameState fails
                gameState = new GameStateMessage();
                gameState.setGameId(gameId);
                gameState.setBoardState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
                gameState.setCurrentTurn("WHITE");
                gameState.setMoveNumber(1);
            }

            sendMessage(client, gameState);

            // Broadcast to other players in the same game
            broadcastGameState(gameId, gameState);

            updateState("Player joined game session: " + gameId, 0);
        } catch (Exception e) {
            sendError(client, "JOIN_GAME_ERROR", "Failed to join game: " + e.getMessage());
        }
    }

    private void handleMove(SocketChannel client, MoveMessage moveMsg) {
        UserSession userSession = userSessions.get(client);
        Long gameId = clientGameSessions.get(client);

        if (userSession == null) {
            sendError(client, "AUTH_REQUIRED", "Must be logged in to make moves");
            return;
        }

        if (gameId == null) {
            sendError(client, "NO_GAME", "Must join a game to make moves");
            return;
        }

        try {
            // Use the correct method names from MoveMessage
            boolean moveValid = sessionManager.processMove(gameId, userSession.getUserId(),
                    moveMsg.getFromSquare(), moveMsg.getToSquare(), moveMsg.getPromotion());

            if (moveValid) {
                // Get updated game state
                GameStateMessage gameState = sessionManager.getGameState(gameId);

                if (gameState != null) {
                    // Broadcast to all players in this game
                    broadcastGameState(gameId, gameState);
                    updateState("Move processed: " + moveMsg.getFromSquare() + " to " + moveMsg.getToSquare(), 0);
                } else {
                    sendError(client, "GAME_STATE_ERROR", "Failed to get updated game state");
                }
            } else {
                sendError(client, "INVALID_MOVE", "Invalid move");
            }
        } catch (Exception e) {
            sendError(client, "MOVE_ERROR", "Error processing move: " + e.getMessage());
        }
    }

    private void handleSpectate(SocketChannel client, SpectateMessage spectateMsg) {
        UserSession userSession = userSessions.get(client);
        if (userSession == null) {
            sendError(client, "AUTH_REQUIRED", "Must be logged in to spectate");
            return;
        }

        try {
            Long gameId = spectateMsg.getGameId();

            // Check if game exists
            GameStateMessage gameState = sessionManager.getGameState(gameId);
            if (gameState == null) {
                sendError(client, "GAME_NOT_FOUND", "Game not found");
                return;
            }

            clientGameSessions.put(client, gameId);

            // Set client mode to spectator
            connections.put(client, ClientMode.SPECTATOR);

            // Add spectator to game session
            sessionManager.addSpectator(gameId, userSession.getUserId());

            // Send current game state
            sendMessage(client, gameState);

            updateState("Spectator joined game: " + gameId, 0);
        } catch (Exception e) {
            sendError(client, "SPECTATE_ERROR", "Failed to spectate game: " + e.getMessage());
        }
    }

    private void broadcastGameState(Long gameId, GameStateMessage gameState) {
        synchronized (connections) {
            for (Map.Entry<SocketChannel, Long> entry : clientGameSessions.entrySet()) {
                if (gameId.equals(entry.getValue())) {
                    try {
                        sendMessage(entry.getKey(), gameState);
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast game state to client: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void broadcastGameDisconnection(Long gameId, Long disconnectedUserId) {
        DisconnectionMessage disconnectionMsg = new DisconnectionMessage(gameId, disconnectedUserId);

        synchronized (connections) {
            for (Map.Entry<SocketChannel, Long> entry : clientGameSessions.entrySet()) {
                if (gameId.equals(entry.getValue())) {
                    try {
                        sendMessage(entry.getKey(), disconnectionMsg);
                    } catch (Exception e) {
                        System.err.println("Failed to broadcast disconnection to client: " + e.getMessage());
                    }
                }
            }
        }
    }

    private void sendMessage(SocketChannel client, Message message) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap((message.toJson() + "\n").getBytes(CHARSET));
            client.write(buffer);
        } catch (IOException e) {
            System.err.println("Failed to send message to client: " + e.getMessage());
        }
    }

    private void sendError(SocketChannel client, String errorCode, String errorMessage) {
        sendMessage(client, new ErrorMessage(errorCode, errorMessage));
    }

    // Utility methods for server management
    public int getConnectedClientsCount() {
        return connectedClients.get();
    }

    public String getServerStatus() {
        return serverStatus.get();
    }

    public boolean isRunning() {
        return server != null && server.isOpen();
    }

    public Map<SocketChannel, ClientMode> getConnections() {
        return new ConcurrentHashMap<>(connections);
    }
}