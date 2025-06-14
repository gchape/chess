package io.gchape.github.service;

import io.gchape.github.model.message.GameStateMessage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;

public class GameSessionManager {
    private final ConcurrentHashMap<Long, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> playerToGame = new ConcurrentHashMap<>();
    private final AtomicLong gameIdGenerator = new AtomicLong(1);
    private final DatabaseService databaseService;

    public GameSessionManager() {
        this.databaseService = DatabaseService.getInstance();
    }

    // Keep the existing constructor for compatibility:
    public GameSessionManager(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }


    public Long createGameSession(Long whitePlayerId, Long blackPlayerId, String gameMode) {
        Long gameId = gameIdGenerator.getAndIncrement();

        try {
            // Record in database
            Long dbGameId = databaseService.createGame(whitePlayerId, blackPlayerId, gameMode);

            GameSession session = new GameSession(dbGameId, whitePlayerId, blackPlayerId, gameMode);
            activeSessions.put(gameId, session);

            if (whitePlayerId != null) {
                playerToGame.put(whitePlayerId, gameId);
            }
            if (blackPlayerId != null) {
                playerToGame.put(blackPlayerId, gameId);
            }

            return gameId;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create game session: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create game session", e);
        }
    }

    public GameSession getGameSession(Long gameId) {
        return activeSessions.get(gameId);
    }

    public Long getPlayerGame(Long playerId) {
        return playerToGame.get(playerId);
    }

    public void addSpectator(Long gameId, Long spectatorId) {
        GameSession session = activeSessions.get(gameId);
        if (session != null) {
            session.addSpectator(spectatorId);
            try {
                databaseService.addSpectator(session.getDatabaseGameId(), spectatorId);
            } catch (SQLException e) {
                System.err.println("SQL error adding spectator: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Error adding spectator: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void endGameSession(Long gameId, String result) {
        GameSession session = activeSessions.get(gameId);
        if (session != null) {
            try {
                databaseService.endGame(session.getDatabaseGameId(), result);

                // Remove players from tracking
                if (session.getWhitePlayerId() != null) {
                    playerToGame.remove(session.getWhitePlayerId());
                }
                if (session.getBlackPlayerId() != null) {
                    playerToGame.remove(session.getBlackPlayerId());
                }

                activeSessions.remove(gameId);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to end game session: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException("Failed to end game session", e);
            }
        }
    }

    // Process move and validate it
    public boolean processMove(Long gameId, Long playerId, String fromPosition, String toPosition, String promotion) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) {
            return false;
        }

        // Check if it's the player's turn
        if (!isPlayerTurn(session, playerId)) {
            return false;
        }

        // Basic validation - in a real chess game, you'd have more complex validation
        if (fromPosition == null || toPosition == null ||
                fromPosition.length() != 2 || toPosition.length() != 2) {
            return false;
        }

        try {
            // Record the move in the session
            session.recordMove(fromPosition, toPosition, "PIECE", null,
                    promotion, false, false, fromPosition + toPosition);

            // Update database - using the compatible overloaded method
            databaseService.recordMove(session.getDatabaseGameId(), session.getMoveCount(),
                    fromPosition, toPosition, "PIECE", null, promotion,
                    false, false, fromPosition + toPosition);

            return true;
        } catch (SQLException e) {
            System.err.println("SQL error recording move: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Error recording move: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Get current game state
    public GameStateMessage getGameState(Long gameId) {
        GameSession session = activeSessions.get(gameId);
        if (session == null) {
            return null;
        }

        GameStateMessage gameState = new GameStateMessage();
        gameState.setGameId(gameId);
        gameState.setBoardState(session.getCurrentBoard() != null ?
                session.getCurrentBoard() :
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        gameState.setCurrentTurn(session.getCurrentTurn());
        gameState.setMoveNumber(session.getMoveCount());
        gameState.setGameEnded(session.isGameEnded());
        gameState.setWhitePlayerId(session.getWhitePlayerId());
        gameState.setBlackPlayerId(session.getBlackPlayerId());
        gameState.setSpectators(session.getSpectators());

        return gameState;
    }

    // Helper method to check if it's the player's turn
    private boolean isPlayerTurn(GameSession session, Long playerId) {
        if (session.getCurrentTurn().equals("WHITE")) {
            return playerId.equals(session.getWhitePlayerId());
        } else {
            return playerId.equals(session.getBlackPlayerId());
        }
    }

    // Join existing game or create new one
    public Long joinOrCreateGame(Long playerId, String gameMode) {
        // First check if player is already in a game
        Long existingGameId = playerToGame.get(playerId);
        if (existingGameId != null) {
            return existingGameId;
        }

        // Look for an existing game waiting for a second player
        for (GameSession session : activeSessions.values()) {
            if (session.getGameMode().equals(gameMode) &&
                    (session.getBlackPlayerId() == null || session.getWhitePlayerId() == null)) {

                // Join this game
                Long gameId = getGameIdBySession(session);
                if (gameId != null) {
                    if (session.getBlackPlayerId() == null) {
                        // Update session to add black player
                        session.setBlackPlayerId(playerId);
                        playerToGame.put(playerId, gameId);

                        try {
                            databaseService.updateGamePlayer(session.getDatabaseGameId(), playerId, "BLACK");
                        } catch (SQLException e) {
                            System.err.println("SQL error updating game player: " + e.getMessage());
                            e.printStackTrace();
                        } catch (Exception e) {
                            System.err.println("Error updating game player: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    return gameId;
                }
            }
        }

        // No existing game found, create new one
        return createGameSession(playerId, null, gameMode);
    }

    // Helper method to get game ID by session
    private Long getGameIdBySession(GameSession targetSession) {
        for (var entry : activeSessions.entrySet()) {
            if (entry.getValue().equals(targetSession)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // Additional methods for compatibility with other potential classes

    /**
     * Get all active game sessions (for admin/monitoring purposes)
     */
    public List<Long> getActiveGameIds() {
        return new ArrayList<>(activeSessions.keySet());
    }

    /**
     * Get number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Check if a game session exists
     */
    public boolean gameExists(Long gameId) {
        return activeSessions.containsKey(gameId);
    }

    /**
     * Force remove a game session (for cleanup purposes)
     */
    public boolean forceRemoveGameSession(Long gameId) {
        GameSession session = activeSessions.remove(gameId);
        if (session != null) {
            // Clean up player tracking
            if (session.getWhitePlayerId() != null) {
                playerToGame.remove(session.getWhitePlayerId());
            }
            if (session.getBlackPlayerId() != null) {
                playerToGame.remove(session.getBlackPlayerId());
            }
            return true;
        }
        return false;
    }

    /**
     * Get all players currently in games
     */
    public List<Long> getActivePlayers() {
        return new ArrayList<>(playerToGame.keySet());
    }
}

class GameSession {
    private final Long databaseGameId;
    private final Long whitePlayerId;
    private Long blackPlayerId;
    private final String gameMode;
    private final List<Long> spectators = new ArrayList<>();
    private String currentBoard; // FEN or board state
    private String currentTurn = "WHITE";
    private int moveCount = 0;
    private boolean gameEnded = false;

    public GameSession(Long databaseGameId, Long whitePlayerId, Long blackPlayerId, String gameMode) {
        this.databaseGameId = databaseGameId;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.gameMode = gameMode;
    }

    public void addSpectator(Long spectatorId) {
        if (!spectators.contains(spectatorId)) {
            spectators.add(spectatorId);
        }
    }

    public void removeSpectator(Long spectatorId) {
        spectators.remove(spectatorId);
    }

    public void recordMove(String fromPos, String toPos, String pieceType, String capturedPiece,
                           String specialMove, boolean isCheck, boolean isCheckmate, String algebraic) {
        moveCount++;
        // Switch turns
        currentTurn = currentTurn.equals("WHITE") ? "BLACK" : "WHITE";

        if (isCheckmate) {
            gameEnded = true;
        }
    }

    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }

    // Getters and Setters
    public Long getDatabaseGameId() { return databaseGameId; }
    public Long getWhitePlayerId() { return whitePlayerId; }
    public Long getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(Long blackPlayerId) { this.blackPlayerId = blackPlayerId; }
    public String getGameMode() { return gameMode; }
    public List<Long> getSpectators() { return new ArrayList<>(spectators); }
    public String getCurrentBoard() { return currentBoard; }
    public void setCurrentBoard(String currentBoard) { this.currentBoard = currentBoard; }
    public String getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(String currentTurn) { this.currentTurn = currentTurn; }
    public int getMoveCount() { return moveCount; }
    public boolean isGameEnded() { return gameEnded; }
}