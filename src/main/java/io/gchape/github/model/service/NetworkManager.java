package io.gchape.github.model.service;

import io.gchape.github.http.client.Client;
import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Consumer;

@Service
public class NetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(NetworkManager.class);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int POLLING_INTERVAL_MS = 100;

    private Client client;
    private Timeline pollingTimeline;
    private Consumer<String> messageHandler;
    private volatile boolean isConnected = false;

    public void connectAsGuest() {
        connectToServer();
    }

    public void connectAsPlayer() {
        connectToServer();
    }

    private void connectToServer() {
        try {
            if (client != null) {
                disconnect();
            }

            client = new Client(SERVER_HOST, SERVER_PORT);
            isConnected = true;
            startPolling();

            logger.info("Connected to server at {}:{}", SERVER_HOST, SERVER_PORT);
        } catch (Exception e) {
            logger.error("Failed to connect to server: {}", e.getMessage(), e);
            isConnected = false;
        }
    }

    public void sendMove(Move move, GameState gameState) {
        if (client != null && isConnected) {
            try {
                Piece piece = gameState.getPieceAt(move.to());
                String moveMessage = String.format("%s#(%d,%d)->(%d,%d)",
                        piece, move.from().row(), move.from().col(),
                        move.to().row(), move.to().col());

                client.send(moveMessage);
                logger.debug("Sent move: {}", moveMessage);

            } catch (Exception e) {
                logger.error("Error sending move: {}", e.getMessage(), e);
            }
        } else {
            logger.warn("Cannot send move - not connected to server");
        }
    }

    private void startPolling() {
        // Stop any existing polling
        stopPolling();

        // Use JavaFX Timeline instead of java.util.Timer
        pollingTimeline = new Timeline(new KeyFrame(
                Duration.millis(POLLING_INTERVAL_MS),
                e -> processIncomingMessages()
        ));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();

        logger.debug("Started polling for messages");
    }

    private void stopPolling() {
        if (pollingTimeline != null) {
            pollingTimeline.stop();
            pollingTimeline = null;
            logger.debug("Stopped polling for messages");
        }
    }

    private void processIncomingMessages() {
        if (client == null || messageHandler == null || !isConnected) {
            return;
        }

        try {
            String message;
            while ((message = client.getIncoming().poll()) != null) {
                final String msg = message;

                // KEY FIX: Always delegate UI updates to JavaFX Application Thread
                Platform.runLater(() -> {
                    try {
                        messageHandler.accept(msg);
                    } catch (Exception e) {
                        logger.error("Error handling message: {}", msg, e);
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error processing incoming messages", e);
        }
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
        logger.debug("Message handler set");
    }

    public void disconnect() {
        isConnected = false;

        stopPolling();

        if (client != null) {
            try {
                client.close();
                logger.info("Disconnected from server");
            } catch (IOException e) {
                logger.error("Error closing client: {}", e.getMessage(), e);
            }
            client = null;
        }
    }

    public boolean isConnected() {
        return isConnected && client != null;
    }

    public String getConnectionStatus() {
        if (isConnected && client != null) {
            return String.format("Connected to %s:%d", SERVER_HOST, SERVER_PORT);
        } else {
            return "Disconnected";
        }
    }

    // Graceful shutdown method
    public void shutdown() {
        logger.info("Shutting down NetworkManager");
        disconnect();
    }
}