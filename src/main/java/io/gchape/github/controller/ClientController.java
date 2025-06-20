package io.gchape.github.controller;

import io.gchape.github.controller.client.Client;
import io.gchape.github.controller.handlers.MouseClickHandlers;
import io.gchape.github.model.ClientModel;
import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import io.gchape.github.model.service.MoveGenerator;
import io.gchape.github.view.ClientView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ClientController implements MouseClickHandlers {
    private final ClientView clientView;
    private final ClientModel clientModel;

    private final GameState gameState;
    private final MoveGenerator moveGenerator;
    private final Timer pollingTimer;

    private Client client;
    private Runnable onGameStateChanged;
    private boolean isInitialized = false;
    private boolean isPlayerTurn = true;

    public ClientController(final ClientView clientView, final ClientModel clientModel) {
        this.clientView = clientView;
        this.clientModel = clientModel;

        this.gameState = new GameState();
        this.moveGenerator = new MoveGenerator();
        this.pollingTimer = new Timer(true);

        setupBindings();
        setOnGameStateChanged(() -> clientView.updateBoard(gameState));
    }

    private void setupBindings() {
        clientModel.emailProperty().bind(clientView.emailProperty());
        clientModel.passwordProperty().bind(clientView.passwordProperty());
        clientModel.usernameProperty().bind(clientView.usernameProperty());
    }

    private void connectToServer() {
        try {
            if (client != null) {
                closeClient();
            }

            client = new Client("localhost", 8080);
            startPollingMessages();

            System.out.println("Connected to server at " + "localhost" + ":" + 8080);
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }

    private void switchToGameView(MouseEvent e) {
        Platform.runLater(() -> {
            Scene scene = ((Node) e.getSource()).getScene();
            Stage stage = (Stage) scene.getWindow();

            scene.setRoot(clientView.createBoard());
            stage.setWidth(800);
            stage.setHeight(800);
        });
    }

    private boolean authenticateUser(String username, String password, String email) {
        // TODO: Implement actual authentication logic

        if (username == null || username.trim().isEmpty()) {
            System.err.println("Username cannot be empty");
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            System.err.println("Password cannot be empty");
            return false;
        }

        System.out.println("Authenticating user: " + username);
        return true;
    }

    private boolean registerUser(String username, String password, String email) {
        // TODO: Implement actual registration logic

        if (username == null || username.trim().isEmpty()) {
            System.err.println("Username cannot be empty");
            return false;
        }

        if (password == null || password.trim().isEmpty()) {
            System.err.println("Password cannot be empty");
            return false;
        }

        if (email == null || email.trim().isEmpty()) {
            System.err.println("Email cannot be empty");
            return false;
        }

        // Placeholder registration - always returns true for now
        System.out.println("Registering user: " + username + " with email: " + email);
        return true;
    }

    public void closeClient() {
        stopPolling();
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setOnGameStateChanged(Runnable callback) {
        this.onGameStateChanged = callback;
    }

    private boolean canSelectPiece(Position position) {
        if (!isPlayerTurn) {
            System.out.println("Not your turn!");
            return false;
        }

        Piece piece = gameState.getPieceAt(position);
        if (piece == null) return false;

        return (gameState.getColor() && piece.color == 'w') ||
                (!gameState.getColor() && piece.color == 'b');
    }

    private List<Position> getValidMoves(Position from) {
        return moveGenerator.generateValidMoves(gameState, from);
    }

    private boolean isValidMove(Move move) {
        List<Position> validMoves = getValidMoves(move.from());
        return validMoves.contains(move.to());
    }

    private boolean makeMove(Move move) {
        if (!isPlayerTurn) {
            System.out.println("Not your turn!");
            return false;
        }

        if (!isValidMove(move)) {
            return false;
        }

        Piece piece = gameState.getPieceAt(move.from());
        if (piece == null) {
            return false;
        }

        gameState.setPieceAt(move.to(), piece);
        gameState.setPieceAt(move.from(), null);

        if (client != null) {
            String moveMessage = String.format("%s#(%d,%d)->(%d,%d)",
                    piece,
                    move.from().row(), move.from().col(),
                    move.to().row(), move.to().col());
            client.send(moveMessage);
        }

        isPlayerTurn = false;

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

            if (!isInitialized && (message.contains("PLAYER:") || message.contains("SPECTATOR:"))) {
                handleClientModeMessage(message);
                isInitialized = true;
                return;
            }

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
                        gameState.setColor(true);
                        isPlayerTurn = true;

                        System.out.println("You are playing as WHITE - Your turn!");
                    } else if ("BLACK".equals(color)) {
                        gameState.setColor(false);
                        isPlayerTurn = false;

                        System.out.println("You are playing as BLACK - Wait for white's move");
                    }
                } else if ("SPECTATOR".equals(mode)) {
                    isPlayerTurn = false;

                    System.out.println("You are spectating");
                }

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

            Piece piece = gameState.getPieceAt(move.from());
            if (piece != null) {
                gameState.setPieceAt(move.to(), piece);
                gameState.setPieceAt(move.from(), null);

                isPlayerTurn = true;

                System.out.println("Received opponent's move: " + pieceCode + " from " + move.from() + " to " + move.to());
                System.out.println("It's now your turn!");

                if (onGameStateChanged != null) {
                    Platform.runLater(onGameStateChanged);
                }
            } else {
                System.err.println("No piece found at position: " + move.from() + " for move: " + moveStr);

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

    @Override
    public void onLoginClicked(MouseEvent e) {
        String username = clientModel.usernameProperty().get();
        String password = clientModel.passwordProperty().get();
        String email = clientModel.emailProperty().get();

        if (authenticateUser(username, password, email)) {
            System.out.println("Login successful for user: " + username);

            // Connect to server after successful authentication
            connectToServer();

            // Switch to game view
            switchToGameView(e);
        } else {
            System.err.println("Login failed for user: " + username);
            // TODO: Show error message to user
        }
    }

    @Override
    public void onGuestClicked(MouseEvent e) {
        System.out.println("Guest login initiated");

        // Connect to server as guest
        connectToServer();

        // Switch to game view
        switchToGameView(e);
    }

    @Override
    public void onRegisterClicked(MouseEvent e) {
        String username = clientModel.usernameProperty().get();
        String password = clientModel.passwordProperty().get();
        String email = clientModel.emailProperty().get();

        if (registerUser(username, password, email)) {
            System.out.println("Registration successful for user: " + username);

            // Connect to server after successful registration
            connectToServer();

            // Switch to game view
            switchToGameView(e);
        } else {
            System.err.println("Registration failed for user: " + username);
            // TODO: Show error message to user
        }
    }

    @Override
    public void onSquareClicked(MouseEvent event) {
        Node source = (Node) event.getSource();
        if (!(source instanceof StackPane square)) return;

        Position clickedPosition = (Position) square.getUserData();

        if (clientView.selectedPosition == null) {
            if (canSelectPiece(clickedPosition)) {
                selectPiece(clickedPosition);
            }
        } else {
            if (clickedPosition.equals(clientView.selectedPosition)) {
                deselectPiece();
            } else if (canSelectPiece(clickedPosition)) {
                deselectPiece();
                selectPiece(clickedPosition);
            } else {
                Move move = new Move(clientView.selectedPosition, clickedPosition);
                if (makeMove(move)) {
                    clientView.updateBoard(gameState);
                    deselectPiece();

                    System.out.println("Move made: " + clientView.selectedPosition + " -> " + clickedPosition);
                } else {
                    System.out.println("Invalid move: " + clientView.selectedPosition + " -> " + clickedPosition);
                }
            }
        }
    }

    private void selectPiece(Position position) {
        clientView.selectedPosition = position;

        StackPane selectedSquare = getSquare(position);
        if (selectedSquare != null) {
            selectedSquare.getStyleClass().add("highlight-pressed");
        }

        List<Position> validMoves = getValidMoves(position);
        for (Position move : validMoves) {
            StackPane target = getSquare(move);
            if (target != null) {
                target.getStyleClass().add("highlight-moves");
                clientView.highlightedSquares.add(target);
            }
        }

        System.out.println("Selected piece at: " + position + " with " + validMoves.size() + " valid moves");
    }

    private void deselectPiece() {
        if (clientView.selectedPosition != null) {
            StackPane selectedSquare = getSquare(clientView.selectedPosition);
            if (selectedSquare != null) {
                selectedSquare.getStyleClass().remove("highlight-pressed");
            }
        }

        clientView.highlightedSquares.forEach(square -> square.getStyleClass().remove("highlight-moves"));
        clientView.highlightedSquares.clear();

        clientView.selectedPosition = null;
        System.out.println("Piece deselected");
    }

    private StackPane getSquare(final Position position) {
        for (Node node : clientView.getBoard().getChildren()) {
            if (node instanceof StackPane square) {
                Position pos = (Position) square.getUserData();
                if (pos.equals(position)) {
                    return square;
                }
            }
        }
        return null;
    }
}