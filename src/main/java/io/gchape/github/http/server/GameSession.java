package io.gchape.github.http.server;

import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameSession {
    private final String gameId;
    private SocketChannel whitePlayer;
    private SocketChannel blackPlayer;
    private final Instant startTime;

    // CRITICAL: Store move history
    private final List<String> moveHistory = new CopyOnWriteArrayList<>();
    private GameStatus status = GameStatus.IN_PROGRESS;
    private String result;

    public enum GameStatus {
        WAITING_FOR_PLAYERS,
        IN_PROGRESS,
        COMPLETED
    }

    public GameSession(String gameId) {
        this.gameId = gameId;
        this.startTime = Instant.now();
    }

    // CRITICAL: Add this method
    public void addMove(String moveMessage) {
        moveHistory.add(moveMessage);
        System.out.println("Added move to game " + gameId + ": " + moveMessage + " (Total: " + moveHistory.size() + ")");
    }

    // CRITICAL: Add this getter
    public List<String> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    // Getters and setters
    public String getGameId() {
        return gameId;
    }

    public SocketChannel getWhitePlayer() {
        return whitePlayer;
    }

    public void setWhitePlayer(SocketChannel whitePlayer) {
        this.whitePlayer = whitePlayer;
    }

    public SocketChannel getBlackPlayer() {
        return blackPlayer;
    }

    public void setBlackPlayer(SocketChannel blackPlayer) {
        this.blackPlayer = blackPlayer;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isWhitePlayer(SocketChannel player) {
        return whitePlayer != null && whitePlayer.equals(player);
    }

    public boolean isBlackPlayer(SocketChannel player) {
        return blackPlayer != null && blackPlayer.equals(player);
    }
}