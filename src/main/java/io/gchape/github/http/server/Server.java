package io.gchape.github.http.server;

import io.gchape.github.model.service.GameSessionManager;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
import java.time.LocalTime;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Enhanced server that integrates with GameSessionManager for game state tracking
 */
@Component
public class Server implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    public static final Charset CHARSET = StandardCharsets.UTF_8;

    private final StringProperty serverStatus;
    private final StringProperty respMessage;
    private final IntegerProperty clientCount;

    private final AtomicReference<String> host;
    private final AtomicReference<Integer> port;

    private ServerSocketChannel serverChannel;
    private Selector clientSelector;
    private Selector acceptSelector;
    private final ExecutorService executorService;

    private volatile boolean running = false;
    private volatile boolean player1 = false;
    private volatile boolean player2 = false;

    private final Map<SocketChannel, Mode> connections;
    private final StringBuilder messageLog;

    private final GameSessionManager gameSessionManager;

    public enum Mode {
        PLAYER, SPECTATOR
    }

    @Autowired
    public Server(GameSessionManager gameSessionManager) {
        this.gameSessionManager = gameSessionManager;

        this.serverStatus = new SimpleStringProperty("Server not started");
        this.respMessage = new SimpleStringProperty("");
        this.clientCount = new SimpleIntegerProperty(0);

        this.host = new AtomicReference<>("localhost");
        this.port = new AtomicReference<>(8080);

        this.connections = new ConcurrentHashMap<>();
        this.messageLog = new StringBuilder();
        this.executorService = Executors.newFixedThreadPool(3); // Increased thread pool size
    }

    public void startServer(String host, int port) {
        if (running) {
            logger.warn("Server is already running");
            return;
        }

        this.host.set(host);
        this.port.set(port);

        try {
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(host, port));

            clientSelector = Selector.open();
            acceptSelector = Selector.open();

            serverChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);

            running = true;
            updateState("Server started on %s:%d".formatted(host, port), 0);

            // Submit separate threads for different operations
            executorService.submit(this::acceptClients);
            executorService.submit(this::watchReadable);

            logger.info("Enhanced server started on {}:{}", host, port);

        } catch (IOException e) {
            updateState("Failed to start server: %s".formatted(e.getMessage()), 0);
            logger.error("Failed to start server", e);
        }
    }

    private void acceptClients() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (acceptSelector.select(1_000) == 0) continue;

                var keys = acceptSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    var key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        acceptClient();
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error in acceptClients", e);
            }
        }
    }

    private void acceptClient() throws IOException {
        var clientChannel = serverChannel.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);

            // Register with the client selector for reading
            clientChannel.register(clientSelector, SelectionKey.OP_READ, ByteBuffer.allocate(1024));

            syncState(clientChannel);
            updateState("Client connected from %s".formatted(clientChannel.getRemoteAddress()), 1);

            // Check if we can start a new game
            checkForNewGameStart();
        }
    }

    private void checkForNewGameStart() {
        if (gameSessionManager != null && player1 && player2) {
            // Find the two player connections
            SocketChannel whitePlayer = null;
            SocketChannel blackPlayer = null;

            for (Map.Entry<SocketChannel, Mode> entry : connections.entrySet()) {
                if (entry.getValue() == Mode.PLAYER) {
                    if (whitePlayer == null) {
                        whitePlayer = entry.getKey();
                    } else if (blackPlayer == null) {
                        blackPlayer = entry.getKey();
                        break;
                    }
                }
            }

            if (whitePlayer != null && blackPlayer != null) {
                gameSessionManager.createGameSession(whitePlayer, blackPlayer);
                logger.info("Started new game session between two players");
            }
        }
    }

    /**
     * Watches for readable client channels and processes incoming messages
     */
    private void watchReadable() {
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                if (clientSelector.select(1_000) == 0) continue;

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
            if (running) {
                logger.error("Error in watchReadable", e);
            }
        }
    }

    private void read(SocketChannel clientChannel, SelectionKey key) throws IOException {
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
            handleMessage(message, clientChannel);
        }

        buffer.clear();
    }

    private void handleMessage(String message, SocketChannel sender) throws IOException {
        synchronized (messageLog) {
            messageLog.append(message).append("\n");
        }

        updateState("Received={ %s } from client.".formatted(message), 0);
        respMessage.set("Server received: %s%n".formatted(message));

        if (gameSessionManager != null && isMoveMessage(message)) {
            gameSessionManager.addMoveToGame(sender, message);
            logger.info("✅ Processed move message: {}", message);
        } else {
            logger.warn("❌ CONDITION FAILED - gameSessionManager: {}, isMoveMessage: {}, message: '{}'",
                    gameSessionManager != null, isMoveMessage(message), message);
        }

        // Check for game ending messages
        if (gameSessionManager != null && isGameEndingMessage(message)) {
            handleGameEndingMessage(message, sender);
        }

        // Broadcast to other clients
        broadcastMessage(message, sender);
    }

    private boolean isMoveMessage(String message) {
        // Check if message matches the move pattern: PIECE#(row,col)->(row,col)
        return message.matches("([A-Za-z]+)#\\(\\d+,\\d+\\)->\\(\\d+,\\d+\\)");
    }

    private boolean isGameEndingMessage(String message) {
        return message.contains("CHECKMATE") ||
                message.contains("RESIGNATION") ||
                message.contains("DRAW") ||
                message.contains("STALEMATE");
    }

    private void handleGameEndingMessage(String message, SocketChannel sender) {
        try {
            // Add the ending message to the game session
            gameSessionManager.addMoveToGame(sender, message);
            logger.info("Game ending message processed: {}", message);
        } catch (Exception e) {
            logger.error("Error handling game ending message", e);
        }
    }

    private void broadcastMessage(String message, SocketChannel sender) throws IOException {
        var broadcastBuffer = ByteBuffer.wrap((message + "\n").getBytes(StandardCharsets.UTF_8));

        synchronized (connections) {
            for (SocketChannel client : connections.keySet()) {
                if (!client.equals(sender) && client.isConnected()) {
                    try {
                        broadcastBuffer.rewind();
                        int bytesWritten = client.write(broadcastBuffer);
                        logger.trace("Broadcasted {} bytes to client: {}", bytesWritten, message);
                    } catch (IOException e) {
                        logger.warn("Failed to broadcast to client: {}", e.getMessage());
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
        Mode mode;
        String modeMessage;

        if (player1 && player2) {
            mode = Mode.SPECTATOR;
            modeMessage = "SPECTATOR:NONE\n";
        } else {
            mode = Mode.PLAYER;

            if (!player1) {
                player1 = true;
                modeMessage = "PLAYER:WHITE\n";
            } else {
                player2 = true;
                modeMessage = "PLAYER:BLACK\n";
            }
        }

        connections.put(clientChannel, mode);

        var modeBuffer = CHARSET.encode(modeMessage);
        while (modeBuffer.hasRemaining()) {
            clientChannel.write(modeBuffer);
        }

        respMessage.set("Sent to client: %s%n".formatted(modeMessage.trim()));

        // Send message history
        synchronized (messageLog) {
            if (!messageLog.isEmpty()) {
                var history = messageLog.toString();
                var historyBuffer = CHARSET.encode(history);

                while (historyBuffer.hasRemaining()) {
                    clientChannel.write(historyBuffer);
                }

                respMessage.set("Sent message history to new client: %s%n".formatted(history.replace("\n", " | ")));
            }
        }
    }

    private void closeClient(SocketChannel clientChannel, SelectionKey key) throws IOException {
        key.cancel();

        var mode = connections.get(clientChannel);
        connections.remove(clientChannel);

        // Handle game session cleanup
        if (gameSessionManager != null) {
            gameSessionManager.handlePlayerDisconnection(clientChannel);
        }

        clientChannel.close();

        updateState("Client disconnected at={ %s }.".formatted(LocalTime.now()), -1);

        if (mode == Mode.PLAYER) {
            if (player1 && player2) {
                // Reset both players when one disconnects to allow new games
                player1 = false;
                player2 = false;
            } else if (player1) {
                player1 = false;
            } else if (player2) {
                player2 = false;
            }
        }

        logger.info("Client disconnected. Players status - Player1: {}, Player2: {}", player1, player2);
    }

    private void updateState(String message, int clientChange) {
        serverStatus.set(message);
        clientCount.set(clientCount.get() + clientChange);
        logger.trace("Server state: {} (clients: {})", message, clientCount.get());
    }

    /**
     * Get current server statistics
     */
    public String getServerStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("Server Status: ").append(serverStatus.get()).append("\n");
        stats.append("Connected Clients: ").append(clientCount.get()).append("\n");
        stats.append("Player 1 Connected: ").append(player1).append("\n");
        stats.append("Player 2 Connected: ").append(player2).append("\n");

        if (gameSessionManager != null) {
            stats.append("Game Sessions: ").append(gameSessionManager.getSessionStats());
        }

        return stats.toString();
    }

    /**
     * Force disconnect all clients (for testing/admin purposes)
     */
    public void disconnectAllClients() {
        synchronized (connections) {
            for (SocketChannel client : connections.keySet()) {
                try {
                    var key = client.keyFor(clientSelector);
                    if (key != null) {
                        closeClient(client, key);
                    }
                } catch (IOException e) {
                    logger.warn("Error disconnecting client", e);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        running = false;

        // Disconnect all clients first
        disconnectAllClients();

        if (executorService != null) {
            executorService.shutdownNow();
        }

        if (clientSelector != null && clientSelector.isOpen()) {
            clientSelector.close();
        }

        if (acceptSelector != null && acceptSelector.isOpen()) {
            acceptSelector.close();
        }

        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }

        logger.info("Enhanced server shut down completely");
    }

    // Property getters
    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public StringProperty respMessageProperty() {
        return respMessage;
    }

    public IntegerProperty clientCountProperty() {
        return clientCount;
    }
}