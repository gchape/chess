package io.gchape.github.model.service;

import io.gchape.github.http.client.Client;
import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

@Service
public class NetworkManager {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int POLLING_INTERVAL_MS = 100;

    private Client client;
    private Timer pollingTimer;
    private Consumer<String> messageHandler;

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
            startPolling();

            System.out.println("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }

    public void sendMove(Move move, GameState gameState) {
        if (client != null) {
            Piece piece = gameState.getPieceAt(move.to());
            String moveMessage = String.format("%s#(%d,%d)->(%d,%d)",
                    piece, move.from().row(), move.from().col(),
                    move.to().row(), move.to().col());
            client.send(moveMessage);
        }
    }

    private void startPolling() {
        pollingTimer = new Timer(true);
        pollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                processIncomingMessages();
            }
        }, 0, POLLING_INTERVAL_MS);
    }

    private void processIncomingMessages() {
        if (client == null || messageHandler == null) return;

        String message;
        while ((message = client.getIncoming().poll()) != null) {
            messageHandler.accept(message);
        }
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public void disconnect() {
        if (pollingTimer != null) {
            pollingTimer.cancel();
            pollingTimer = null;
        }

        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
            client = null;
        }
    }
}