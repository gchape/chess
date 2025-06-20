package io.gchape.github.model.service;

import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GameService {
    private final GameState gameState;
    private final MoveGenerator moveGenerator;

    private Position selectedPosition;
    private boolean isPlayerTurn = true;
    private boolean isInitialized = false;
    private Runnable onGameStateChanged;

    public GameService() {
        this.gameState = new GameState();
        this.moveGenerator = new MoveGenerator();
    }

    public boolean canSelectPiece(Position position) {
        if (!isPlayerTurn) return false;

        Piece piece = gameState.getPieceAt(position);
        if (piece == null) return false;

        return isPlayersPiece(piece);
    }

    private boolean isPlayersPiece(Piece piece) {
        return (gameState.getColor() && piece.color == 'w') ||
                (!gameState.getColor() && piece.color == 'b');
    }

    public void selectPiece(Position position) {
        this.selectedPosition = position;
        System.out.println("Selected piece at: " + position);
    }

    public void deselectPiece() {
        this.selectedPosition = null;
        System.out.println("Piece deselected");
    }

    public boolean hasSelection() {
        return selectedPosition != null;
    }

    public boolean isSelectedPosition(Position position) {
        return selectedPosition != null && selectedPosition.equals(position);
    }

    public Position getSelectedPosition() {
        return selectedPosition;
    }

    public List<Position> getValidMoves(Position from) {
        return moveGenerator.generateValidMoves(gameState, from);
    }

    public boolean makeMove(Move move) {
        if (!canMakeMove(move)) return false;

        executeMoveOnBoard(move);
        updateGameState();
        return true;
    }

    private boolean canMakeMove(Move move) {
        if (!isPlayerTurn) return false;
        if (gameState.getPieceAt(move.from()) == null) return false;

        List<Position> validMoves = getValidMoves(move.from());
        return validMoves.contains(move.to());
    }

    private void executeMoveOnBoard(Move move) {
        Piece piece = gameState.getPieceAt(move.from());
        gameState.setPieceAt(move.to(), piece);
        gameState.setPieceAt(move.from(), null);
    }

    private void updateGameState() {
        isPlayerTurn = false;
        notifyGameStateChanged();
    }

    public void handleOpponentMove(String moveMessage) {
        try {
            Move move = parseMoveFromMessage(moveMessage);
            Piece piece = gameState.getPieceAt(move.from());

            if (piece != null) {
                gameState.setPieceAt(move.to(), piece);
                gameState.setPieceAt(move.from(), null);
                isPlayerTurn = true;
                notifyGameStateChanged();
                System.out.println("Opponent moved: " + move.from() + " -> " + move.to());
            }
        } catch (Exception e) {
            System.err.println("Failed to process opponent move: " + e.getMessage());
        }
    }

    public void handleInitialization(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length >= 2) {
                String mode = parts[0].trim();
                String color = parts[1].trim();

                if ("PLAYER".equals(mode)) {
                    setupPlayerMode(color);
                } else if ("SPECTATOR".equals(mode)) {
                    setupSpectatorMode();
                }

                isInitialized = true;
                notifyGameStateChanged();
            }
        } catch (Exception e) {
            System.err.println("Failed to handle initialization: " + e.getMessage());
        }
    }

    private void setupPlayerMode(String color) {
        if ("WHITE".equals(color)) {
            gameState.setColor(true);
            isPlayerTurn = true;
            System.out.println("Playing as WHITE");
        } else if ("BLACK".equals(color)) {
            gameState.setColor(false);
            isPlayerTurn = false;
            System.out.println("Playing as BLACK");
        }
    }

    private void setupSpectatorMode() {
        isPlayerTurn = false;
        System.out.println("Spectating game");
    }

    public boolean isInitializationMessage(String message) {
        return !isInitialized && (message.contains("PLAYER:") || message.contains("SPECTATOR:"));
    }

    public boolean isMoveMessage(String message) {
        return message.contains("#") && message.contains("->");
    }

    private Move parseMoveFromMessage(String moveStr) {
        String[] parts = moveStr.split("#")[1].split("->");
        Position from = parsePosition(parts[0].trim());
        Position to = parsePosition(parts[1].trim());
        return new Move(from, to);
    }

    private Position parsePosition(String posStr) {
        String cleaned = posStr.replaceAll("[\\(\\)]", "");
        String[] coords = cleaned.split(",");
        int row = Integer.parseInt(coords[0].trim());
        int col = Integer.parseInt(coords[1].trim());
        return new Position(row, col);
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setOnGameStateChanged(Runnable callback) {
        this.onGameStateChanged = callback;
    }

    private void notifyGameStateChanged() {
        if (onGameStateChanged != null) {
            javafx.application.Platform.runLater(onGameStateChanged);
        }
    }

    public void cleanup() {

    }
}
