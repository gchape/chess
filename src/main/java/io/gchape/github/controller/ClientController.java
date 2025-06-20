package io.gchape.github.controller;

import io.gchape.github.http.client.Client;
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
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_HOST = "localhost";

    private static final int POLLING_INTERVAL_MS = 100;

    private static final int GAME_WINDOW_WIDTH = 800;
    private static final int GAME_WINDOW_HEIGHT = 800;

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

        initializeController();
    }

    private void initializeController() {
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

            establishConnection();

            System.out.println("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
        } catch (Exception e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }

    private void establishConnection() {
        client = new Client(SERVER_HOST, SERVER_PORT);

        startPollingMessages();
    }

    public void closeClient() {
        stopPolling();

        closeClientConnection();
    }

    private void closeClientConnection() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void switchToGameView(MouseEvent e) {
        Platform.runLater(() -> {
            Stage stage = getStageFromEvent(e);
            Scene scene = stage.getScene();

            updateSceneForGame(scene);
            configureGameWindow(stage);
        });
    }

    private Stage getStageFromEvent(MouseEvent e) {
        Scene scene = ((Node) e.getSource()).getScene();
        return (Stage) scene.getWindow();
    }

    private void updateSceneForGame(Scene scene) {
        scene.setRoot(clientView.createBoard());
    }

    private void configureGameWindow(Stage stage) {
        stage.setWidth(GAME_WINDOW_WIDTH);
        stage.setHeight(GAME_WINDOW_HEIGHT);
    }

    private void setOnGameStateChanged(Runnable callback) {
        this.onGameStateChanged = callback;
    }

    private boolean canSelectPiece(Position position) {
        if (!isPlayerTurn) {
            System.out.println("Not your turn!");
            return false;
        }

        return isPieceOwnedByPlayer(position);
    }

    private boolean isPieceOwnedByPlayer(Position position) {
        Piece piece = gameState.getPieceAt(position);
        if (piece == null) return false;

        return isPlayersPiece(piece);
    }

    private boolean isPlayersPiece(Piece piece) {
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
        if (!canMakeMove(move)) {
            return false;
        }

        executeMoveOnBoard(move);
        sendMoveToServer(move);
        updateGameState();

        return true;
    }

    private boolean canMakeMove(Move move) {
        if (!isPlayerTurn) {
            System.out.println("Not your turn!");
            return false;
        }

        if (!isValidMove(move)) {
            return false;
        }

        return gameState.getPieceAt(move.from()) != null;
    }

    private void executeMoveOnBoard(Move move) {
        Piece piece = gameState.getPieceAt(move.from());
        gameState.setPieceAt(move.to(), piece);
        gameState.setPieceAt(move.from(), null);
    }

    private void sendMoveToServer(Move move) {
        if (client != null) {
            String moveMessage = formatMoveMessage(move);
            client.send(moveMessage);
        }
    }

    private String formatMoveMessage(Move move) {
        Piece piece = gameState.getPieceAt(move.to());
        return String.format("%s#(%d,%d)->(%d,%d)",
                piece,
                move.from().row(), move.from().col(),
                move.to().row(), move.to().col());
    }

    private void updateGameState() {
        isPlayerTurn = false;
        notifyGameStateChanged();
    }

    private void notifyGameStateChanged() {
        if (onGameStateChanged != null) {
            Platform.runLater(onGameStateChanged);
        }
    }

    private void startPollingMessages() {
        pollingTimer.scheduleAtFixedRate(createPollingTask(), 0, POLLING_INTERVAL_MS);
    }

    private TimerTask createPollingTask() {
        return new TimerTask() {
            @Override
            public void run() {
                processIncomingMessages();
            }
        };
    }

    private void processIncomingMessages() {
        if (client == null) return;

        String message;
        while ((message = client.getIncoming().poll()) != null) {
            handleIncomingMessage(message);
        }
    }

    private void handleIncomingMessage(String message) {
        try {
            System.out.println("Processing message: " + message);

            if (isInitializationMessage(message)) {
                handleInitializationMessage(message);
                return;
            }

            if (isMoveMessage(message)) {
                handleMoveMessage(message);
            }
        } catch (Exception e) {
            logMessageHandlingError(message, e);
        }
    }

    private boolean isInitializationMessage(String message) {
        return !isInitialized &&
                (message.contains("PLAYER:") ||
                        message.contains("SPECTATOR:"));
    }

    private void handleInitializationMessage(String message) {
        handleClientModeMessage(message);
        isInitialized = true;
    }

    private boolean isMoveMessage(String message) {
        return message.contains("#") && message.contains("->");
    }

    private void logMessageHandlingError(String message, Exception e) {
        System.err.println("Failed to handle incoming message: " + message + " - " + e.getMessage());
    }

    private void handleClientModeMessage(String message) {
        try {
            String[] parts = parseClientModeMessage(message);
            if (parts.length >= 2) {
                processClientMode(parts[0].trim(), parts[1].trim());
            }
        } catch (Exception e) {
            System.err.println("Failed to handle client mode message: " + message);
        }
    }

    private String[] parseClientModeMessage(String message) {
        return message.split(":");
    }

    private void processClientMode(String mode, String color) {
        if ("PLAYER".equals(mode)) {
            handlePlayerMode(color);
        } else if ("SPECTATOR".equals(mode)) {
            handleSpectatorMode();
        }

        notifyGameStateChanged();
    }

    private void handlePlayerMode(String color) {
        if ("WHITE".equals(color)) {
            setupWhitePlayer();
        } else if ("BLACK".equals(color)) {
            setupBlackPlayer();
        }
    }

    private void setupWhitePlayer() {
        gameState.setColor(true);
        isPlayerTurn = true;

        System.out.println("You are playing as WHITE - Your turn!");
    }

    private void setupBlackPlayer() {
        gameState.setColor(false);
        isPlayerTurn = false;

        System.out.println("You are playing as BLACK - Wait for white's move");
    }

    private void handleSpectatorMode() {
        isPlayerTurn = false;

        System.out.println("You are spectating");
    }

    private void handleMoveMessage(String moveStr) {
        try {
            System.out.println("Handling move message: " + moveStr);

            Move move = parseMoveFromMessage(moveStr);
            String pieceCode = extractPieceCodeFromMessage(moveStr);

            processOpponentMove(move, pieceCode, moveStr);
        } catch (Exception e) {
            System.err.println("Failed to parse move: " + moveStr + " - " + e.getMessage());
        }
    }

    private Move parseMoveFromMessage(String moveStr) {
        String[] parts = moveStr.split("#");
        validateMoveMessageFormat(parts, moveStr, "#");

        String movePart = parts[1].trim();
        String[] moveParts = movePart.split("->");
        validateMoveMessageFormat(moveParts, moveStr, "->");

        Position from = parsePosition(moveParts[0].trim());
        Position to = parsePosition(moveParts[1].trim());

        return new Move(from, to);
    }

    private String extractPieceCodeFromMessage(String moveStr) {
        return moveStr.split("#")[0].trim();
    }

    private void validateMoveMessageFormat(String[] parts, String moveStr, String delimiter) {
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid move format (missing " + delimiter + "): " + moveStr);
        }
    }

    private void processOpponentMove(Move move, String pieceCode, String originalMessage) {
        Piece piece = gameState.getPieceAt(move.from());

        if (piece != null) {
            executeOpponentMove(move, pieceCode);
        }
    }

    private void executeOpponentMove(Move move, String pieceCode) {
        Piece piece = gameState.getPieceAt(move.from());
        gameState.setPieceAt(move.to(), piece);
        gameState.setPieceAt(move.from(), null);

        isPlayerTurn = true;
        logOpponentMove(pieceCode, move);
        notifyGameStateChanged();
    }

    private void logOpponentMove(String pieceCode, Move move) {
        System.out.println("Received opponent's move: " + pieceCode + " from " + move.from() + " to " + move.to());
        System.out.println("It's now your turn!");
    }

    private Position parsePosition(String posStr) {
        String cleaned = posStr.replaceAll("[\\(\\)]", "");
        String[] coords = cleaned.split(",");

        validatePositionFormat(coords, posStr);

        int row = Integer.parseInt(coords[0].trim());
        int col = Integer.parseInt(coords[1].trim());

        return new Position(row, col);
    }

    private void validatePositionFormat(String[] coords, String posStr) {
        if (coords.length != 2) {
            throw new IllegalArgumentException("Invalid position format: " + posStr);
        }
    }

    private void stopPolling() {
        pollingTimer.cancel();
    }

    @Override
    public void onLoginClicked(MouseEvent e) {

    }

    @Override
    public void onGuestClicked(MouseEvent e) {
        connectToServer();
        switchToGameView(e);
    }

    @Override
    public void onRegisterClicked(MouseEvent e) {
    }

    private void handleSuccessfulLogin(String username, MouseEvent e) {
        System.out.println("Login successful for user: " + username);

        connectToServer();
        switchToGameView(e);
    }

    private void handleFailedLogin(String username) {
        System.err.println("Login failed for user: " + username);

        // TODO: Show error message to user
    }

    private void handleSuccessfulRegistration(String username, MouseEvent e) {
        System.out.println("Registration successful for user: " + username);

        // TODO: Show success message to user
    }

    @Override
    public void onSquareClicked(MouseEvent event) {
        StackPane square = getSquareFromEvent(event);
        if (square == null) return;

        Position clickedPosition = (Position) square.getUserData();

        if (clientView.selectedPosition == null) {
            handleSquareClickWithoutSelection(clickedPosition);
        } else {
            handleSquareClickWithSelection(clickedPosition);
        }
    }

    private StackPane getSquareFromEvent(MouseEvent event) {
        Node source = (Node) event.getSource();
        return (source instanceof StackPane) ? (StackPane) source : null;
    }

    private void handleSquareClickWithoutSelection(Position clickedPosition) {
        if (canSelectPiece(clickedPosition)) {
            selectPiece(clickedPosition);
        }
    }

    private void handleSquareClickWithSelection(Position clickedPosition) {
        if (clickedPosition.equals(clientView.selectedPosition)) {
            deselectPiece();
        } else if (canSelectPiece(clickedPosition)) {
            reselectPiece(clickedPosition);
        } else {
            attemptMove(clickedPosition);
        }
    }

    private void reselectPiece(Position clickedPosition) {
        deselectPiece();
        selectPiece(clickedPosition);
    }

    private void attemptMove(Position clickedPosition) {
        Move move = new Move(clientView.selectedPosition, clickedPosition);

        if (makeMove(move)) {
            handleSuccessfulMove(move);
        } else {
            handleFailedMove(move);
        }
    }

    private void handleSuccessfulMove(Move move) {
        clientView.updateBoard(gameState);
        deselectPiece();
        System.out.println("Move made: " + move.from() + " -> " + move.to());
    }

    private void handleFailedMove(Move move) {
        System.out.println("Invalid move: " + move.from() + " -> " + move.to());
    }

    private void selectPiece(Position position) {
        clientView.selectedPosition = position;

        highlightSelectedSquare(position);
        highlightValidMoves(position);

        logPieceSelection(position);
    }

    private void highlightSelectedSquare(Position position) {
        StackPane selectedSquare = getSquare(position);
        if (selectedSquare != null) {
            selectedSquare.getStyleClass().add("highlight-pressed");
        }
    }

    private void highlightValidMoves(Position position) {
        List<Position> validMoves = getValidMoves(position);

        for (Position move : validMoves) {
            highlightMoveSquare(move);
        }
    }

    private void highlightMoveSquare(Position move) {
        StackPane target = getSquare(move);
        if (target != null) {
            target.getStyleClass().add("highlight-moves");
            clientView.highlightedSquares.add(target);
        }
    }

    private void logPieceSelection(Position position) {
        List<Position> validMoves = getValidMoves(position);

        System.out.println("Selected piece at: " + position + " with " + validMoves.size() + " valid moves");
    }

    private void deselectPiece() {
        removeSelectedSquareHighlight();
        clearMoveHighlights();
        clearSelection();

        System.out.println("Piece deselected");
    }

    private void removeSelectedSquareHighlight() {
        if (clientView.selectedPosition != null) {
            StackPane selectedSquare = getSquare(clientView.selectedPosition);
            if (selectedSquare != null) {
                selectedSquare.getStyleClass().remove("highlight-pressed");
            }
        }
    }

    private void clearMoveHighlights() {
        clientView.highlightedSquares.forEach(square ->
                square.getStyleClass().remove("highlight-moves"));
        clientView.highlightedSquares.clear();
    }

    private void clearSelection() {
        clientView.selectedPosition = null;
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