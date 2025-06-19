package io.gchape.github.controller;

import io.gchape.github.controller.client.Client;
import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import io.gchape.github.model.service.MoveGenerator;
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ClientController {
    private final GameState gameState;
    private final MoveGenerator moveGenerator;
    private final Timer pollingTimer;

    private Client client;
    private Runnable onGameStateChanged;
    private boolean isInitialized = false;
    private boolean isPlayerTurn = true;

    public ClientController() {
        this.gameState = new GameState();
        this.moveGenerator = new MoveGenerator();
        this.pollingTimer = new Timer(true);
    }

    public void startClient(final String host, final int port) {
        client = new Client(host, port);
        startPollingMessages();
    }

    public void closeClient() {
        stopPolling();
        try {
            if (client != null) {
                client.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setOnGameStateChanged(Runnable callback) {
        this.onGameStateChanged = callback;
    }

    public boolean canSelectPiece(Position position) {
        if (!isPlayerTurn) {
            System.out.println("Not your turn!");
            return false;
        }

        Piece piece = gameState.getPieceAt(position);
        if (piece == null) return false;

        return (gameState.isWhite() && piece.color == 'w') ||
                (!gameState.isWhite() && piece.color == 'b');
    }

    public List<Position> getValidMoves(Position from) {
        return moveGenerator.generateValidMoves(gameState, from);
    }

    public boolean isValidMove(Move move) {
        List<Position> validMoves = getValidMoves(move.from());
        return validMoves.contains(move.to());
    }

    public boolean makeMove(Move move) {
        if (!isPlayerTurn) {
            System.out.println("Not your turn!");
            return false;
        }

        if (!isValidMove(move)) {
            return false;
        }

        // Get the piece before moving it
        Piece piece = gameState.getPieceAt(move.from());
        if (piece == null) {
            return false;
        }

        // Apply move locally first
        gameState.setPieceAt(move.to(), piece);
        gameState.setPieceAt(move.from(), null);

        // Send move to server in your format: piece#(row,col)->(row,col)
        if (client != null) {
            String moveMessage = String.format("%s#(%d,%d)->(%d,%d)",
                    piece,
                    move.from().row(), move.from().col(),
                    move.to().row(), move.to().col());
            client.send(moveMessage);
        }

        // It's no longer our turn after making a move
        isPlayerTurn = false;

        // Notify UI of change
        if (onGameStateChanged != null) {
            Platform.runLater(onGameStateChanged);
        }

        return true;
    }

    private void startPollingMessages() {
        pollingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (client == null) return;

                String message;
                while ((message = client.getIncoming().poll()) != null) {
                    handleIncomingMessage(message);
                }
            }
        }, 0, 100); // Poll more frequently for better responsiveness
    }

    private void handleIncomingMessage(String message) {
        try {
            System.out.println("Processing message: " + message);

            // Handle client mode initialization
            if (!isInitialized && (message.contains("PLAYER:") || message.contains("SPECTATOR:"))) {
                handleClientModeMessage(message);
                isInitialized = true;
                return;
            }

            // Handle move messages in format: piece#(row,col)->(row,col)
            if (message.contains("#") && message.contains("->")) {
                handleMoveMessage(message);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle incoming message: " + message + " - " + e.getMessage());
        }
    }

    private void handleClientModeMessage(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                String mode = parts[0].trim();
                String color = parts[1].trim();

                if ("PLAYER".equals(mode)) {
                    if ("WHITE".equals(color)) {
                        gameState.setWhite(true);
                        isPlayerTurn = true;

                        System.out.println("You are playing as WHITE - Your turn!");
                    } else if ("BLACK".equals(color)) {
                        gameState.setWhite(false);
                        isPlayerTurn = false;

                        System.out.println("You are playing as BLACK - Wait for white's move");
                    }
                } else if ("SPECTATOR".equals(mode)) {
                    isPlayerTurn = false;

                    System.out.println("You are spectating");
                }

                // Notify UI that game state has been initialized
                if (onGameStateChanged != null) {
                    Platform.runLater(onGameStateChanged);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to handle client mode message: " + message);
        }
    }

    private void handleMoveMessage(String moveStr) {
        try {
            System.out.println("Handling move message: " + moveStr);

            // Parse move format: "piece#(row,col)->(row,col)"
            String[] parts = moveStr.split("#");
            if (parts.length != 2) {
                System.err.println("Invalid move format (missing #): " + moveStr);
                return;
            }

            String pieceCode = parts[0].trim();
            String movePart = parts[1].trim();

            String[] moveParts = movePart.split("->");
            if (moveParts.length != 2) {
                System.err.println("Invalid move format (missing ->): " + moveStr);
                return;
            }

            Position from = parsePosition(moveParts[0].trim());
            Position to = parsePosition(moveParts[1].trim());
            Move move = new Move(from, to);

            // Apply the move received from server (this is an opponent's move)
            Piece piece = gameState.getPieceAt(move.from());
            if (piece != null) {
                gameState.setPieceAt(move.to(), piece);
                gameState.setPieceAt(move.from(), null);

                // Now it's our turn after opponent's move
                isPlayerTurn = true;

                System.out.println("Received opponent's move: " + pieceCode + " from " + move.from() + " to " + move.to());
                System.out.println("It's now your turn!");

                // Update UI
                if (onGameStateChanged != null) {
                    Platform.runLater(onGameStateChanged);
                }
            } else {
                System.err.println("No piece found at position: " + move.from() + " for move: " + moveStr);
                // Try to create the piece based on the piece code
                try {
                    Piece expectedPiece = Piece.fromCode(pieceCode);
                    gameState.setPieceAt(move.to(), expectedPiece);
                    isPlayerTurn = true;

                    System.out.println("Created missing piece: " + expectedPiece + " at " + move.to());

                    if (onGameStateChanged != null) {
                        Platform.runLater(onGameStateChanged);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Could not create piece from code: " + pieceCode);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse move: " + moveStr + " - " + e.getMessage());
        }
    }

    private Position parsePosition(String posStr) {
        // Parse format: "(row,col)"
        String cleaned = posStr.replaceAll("[\\(\\)]", "");
        String[] coords = cleaned.split(",");
        if (coords.length != 2) {
            throw new IllegalArgumentException("Invalid position format: " + posStr);
        }

        int row = Integer.parseInt(coords[0].trim());
        int col = Integer.parseInt(coords[1].trim());
        return new Position(row, col);
    }

    private void stopPolling() {
        pollingTimer.cancel();
    }
}