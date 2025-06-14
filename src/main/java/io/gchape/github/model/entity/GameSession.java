package io.gchape.github.model.entity;



import io.gchape.github.model.PieceColor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity class representing a game session with players and spectators.
 */
public class GameSession {
    private Long gameId;
    private Long whitePlayerId;
    private Long blackPlayerId;
    private String gameMode;
    private String gameResult;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String currentTurn;
    private String boardState; // JSON representation or FEN notation
    private int moveCount;
    private boolean gameEnded;
    private String gameStatus; // "WAITING", "IN_PROGRESS", "FINISHED"

    // Runtime data (not persisted)
    private final Set<Long> spectators;
    private final Map<String, Object> gameMetadata;
    private String lastMove;
    private boolean isCheck;
    private boolean isCheckmate;
    private boolean isStalemate;

    // Default constructor
    public GameSession() {
        this.spectators = ConcurrentHashMap.newKeySet();
        this.gameMetadata = new ConcurrentHashMap<>();
        this.currentTurn = PieceColor.WHITE.toString();
        this.moveCount = 0;
        this.gameEnded = false;
        this.gameStatus = "WAITING";
        this.startTime = LocalDateTime.now();
    }

    // Constructor for new game
    public GameSession(Long whitePlayerId, Long blackPlayerId, String gameMode) {
        this();
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.gameMode = gameMode;
        this.gameStatus = "IN_PROGRESS";
    }

    // Full constructor (for database retrieval)
    public GameSession(Long gameId, Long whitePlayerId, Long blackPlayerId,
                       String gameMode, String gameResult, LocalDateTime startTime,
                       LocalDateTime endTime, String currentTurn, String boardState,
                       int moveCount, boolean gameEnded, String gameStatus) {
        this();
        this.gameId = gameId;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.gameMode = gameMode;
        this.gameResult = gameResult;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentTurn = currentTurn;
        this.boardState = boardState;
        this.moveCount = moveCount;
        this.gameEnded = gameEnded;
        this.gameStatus = gameStatus;
    }

    // Getters and Setters
    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public Long getWhitePlayerId() {
        return whitePlayerId;
    }

    public void setWhitePlayerId(Long whitePlayerId) {
        this.whitePlayerId = whitePlayerId;
    }

    public Long getBlackPlayerId() {
        return blackPlayerId;
    }

    public void setBlackPlayerId(Long blackPlayerId) {
        this.blackPlayerId = blackPlayerId;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public String getGameResult() {
        return gameResult;
    }

    public void setGameResult(String gameResult) {
        this.gameResult = gameResult;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public void setCurrentTurn(String currentTurn) {
        this.currentTurn = currentTurn;
    }

    public String getBoardState() {
        return boardState;
    }

    public void setBoardState(String boardState) {
        this.boardState = boardState;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
    }

    public boolean isGameEnded() {
        return gameEnded;
    }

    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(String gameStatus) {
        this.gameStatus = gameStatus;
    }

    public Set<Long> getSpectators() {
        return new HashSet<>(spectators);
    }

    public String getLastMove() {
        return lastMove;
    }

    public void setLastMove(String lastMove) {
        this.lastMove = lastMove;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public boolean isCheckmate() {
        return isCheckmate;
    }

    public void setCheckmate(boolean checkmate) {
        isCheckmate = checkmate;
    }

    public boolean isStalemate() {
        return isStalemate;
    }

    public void setStalemate(boolean stalemate) {
        isStalemate = stalemate;
    }

    // Utility methods
    public void addSpectator(Long userId) {
        spectators.add(userId);
    }

    public void removeSpectator(Long userId) {
        spectators.remove(userId);
    }

    public boolean isSpectator(Long userId) {
        return spectators.contains(userId);
    }

    public boolean isPlayer(Long userId) {
        return Objects.equals(whitePlayerId, userId) || Objects.equals(blackPlayerId, userId);
    }

    public PieceColor getPlayerColor(Long userId) {
        if (Objects.equals(whitePlayerId, userId)) {
            return PieceColor.WHITE;
        } else if (Objects.equals(blackPlayerId, userId)) {
            return PieceColor.BLACK;
        }
        return null; // Spectator or not in game
    }

    public void switchTurn() {
        this.currentTurn = currentTurn.equals(PieceColor.WHITE.toString())
                ? PieceColor.BLACK.toString()
                : PieceColor.WHITE.toString();
    }

    public void incrementMoveCount() {
        this.moveCount++;
    }

    public void endGame(String result) {
        this.gameResult = result;
        this.gameEnded = true;
        this.gameStatus = "FINISHED";
        this.endTime = LocalDateTime.now();
    }

    public long getGameDurationMinutes() {
        if (startTime == null) return 0;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).toMinutes();
    }

    public void setMetadata(String key, Object value) {
        gameMetadata.put(key, value);
    }

    public Object getMetadata(String key) {
        return gameMetadata.get(key);
    }

    public List<Long> getAllParticipants() {
        List<Long> participants = new ArrayList<>();
        if (whitePlayerId != null) participants.add(whitePlayerId);
        if (blackPlayerId != null) participants.add(blackPlayerId);
        participants.addAll(spectators);
        return participants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameSession that = (GameSession) o;
        return Objects.equals(gameId, that.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId);
    }

    @Override
    public String toString() {
        return "GameSession{" +
                "gameId=" + gameId +
                ", whitePlayerId=" + whitePlayerId +
                ", blackPlayerId=" + blackPlayerId +
                ", gameMode='" + gameMode + '\'' +
                ", gameResult='" + gameResult + '\'' +
                ", currentTurn='" + currentTurn + '\'' +
                ", moveCount=" + moveCount +
                ", gameStatus='" + gameStatus + '\'' +
                ", spectators=" + spectators.size() +
                '}';
    }
}