package io.gchape.github.model.service;

import io.gchape.github.http.server.GameSession;
import io.gchape.github.model.entity.db.Player;
import io.gchape.github.model.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.channels.SocketChannel;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active game sessions and coordinates with PGNService for game saving
 */
@Service
public class GameSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(GameSessionManager.class);

    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<SocketChannel, String> playerToGameMap = new ConcurrentHashMap<>();
    private final Map<SocketChannel, Integer> playerIds = new ConcurrentHashMap<>();

    private final PGNService pgnService;
    private final PlayerRepository playerRepository;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public GameSessionManager(PGNService pgnService, PlayerRepository playerRepository) {
        this.pgnService = pgnService;
        this.playerRepository = playerRepository;
    }

    /**
     * Creates a new game session when two players connect
     */
    public void createGameSession(SocketChannel whitePlayer, SocketChannel blackPlayer) {
        String gameId = UUID.randomUUID().toString();
        GameSession session = new GameSession(gameId);

        session.setWhitePlayer(whitePlayer);
        session.setBlackPlayer(blackPlayer);

        // Store the session immediately
        activeSessions.put(gameId, session);
        playerToGameMap.put(whitePlayer, gameId);
        playerToGameMap.put(blackPlayer, gameId);

        logger.info("Created new game session: {} with players", gameId);

        // Create anonymous players synchronously and verify creation
        boolean playersCreated = assignPlayerIds(whitePlayer, blackPlayer);

        if (playersCreated) {
            logger.info("Game session {} ready with player IDs assigned", gameId);
        } else {
            logger.warn("Game session {} created but player ID assignment failed", gameId);
        }

    }

    /**
     * Assigns player IDs from database or creates anonymous players
     * Returns true if both players were successfully assigned IDs
     */
    private boolean assignPlayerIds(SocketChannel whitePlayer, SocketChannel blackPlayer) {
        try {
            long timestamp = System.currentTimeMillis();
            int randomNum = random.nextInt(1000);

            String whiteUsername = "White_" + timestamp + "_" + randomNum;
            String blackUsername = "Black_" + timestamp + "_" + (randomNum + 1);

            logger.debug("Creating anonymous players: {} and {}", whiteUsername, blackUsername);

            Player whiteAnonymous = findOrCreateAnonymousPlayer(whiteUsername);
            Player blackAnonymous = findOrCreateAnonymousPlayer(blackUsername);

            boolean success = true;

            if (whiteAnonymous != null) {
                playerIds.put(whitePlayer, whiteAnonymous.id());
                logger.info("Assigned white player ID: {} ({})", whiteAnonymous.id(), whiteAnonymous.username());
            } else {
                logger.error("Failed to create white anonymous player");
                success = false;
            }

            if (blackAnonymous != null) {
                playerIds.put(blackPlayer, blackAnonymous.id());
                logger.info("Assigned black player ID: {} ({})", blackAnonymous.id(), blackAnonymous.username());
            } else {
                logger.error("Failed to create black anonymous player");
                success = false;
            }

            return success;

        } catch (Exception e) {
            logger.error("Error assigning player IDs", e);
            return false;
        }
    }

    /**
     * /**
     * Adds a move to the active game session
     */
    public void addMoveToGame(SocketChannel player, String moveMessage) {
        String gameId = playerToGameMap.get(player);
        if (gameId != null) {
            GameSession session = activeSessions.get(gameId);
            if (session != null && session.getStatus() == GameSession.GameStatus.IN_PROGRESS) {
                // CRITICAL: Call addMove on the session
                session.addMove(moveMessage);
                logger.debug("Added move to game {}: {} (Total moves: {})",
                        gameId, moveMessage, session.getMoveHistory().size());

                // Check if this move ends the game
                if (isGameEndingMove(moveMessage)) {
                    completeGame(session, determineGameResult(moveMessage));
                }
            }
        } else {
            logger.warn("No game session found for player when adding move: {}", moveMessage);
        }
    }

    /**
     * Handles player disconnection
     */
    public void handlePlayerDisconnection(SocketChannel player) {
        String gameId = playerToGameMap.get(player);
        if (gameId != null) {
            GameSession session = activeSessions.get(gameId);
            if (session != null && session.getStatus() == GameSession.GameStatus.IN_PROGRESS) {
                // Mark game as abandoned due to disconnection
                String result = session.isWhitePlayer(player) ? "0-1" : "1-0"; // Opponent wins
                completeGame(session, result + " (disconnection)");
            }

            // Clean up player mapping
            playerToGameMap.remove(player);
            playerIds.remove(player);
        }
    }

    /**
     * Completes a game and saves it to the database
     */
    private void completeGame(GameSession session, String result) {
        session.setStatus(GameSession.GameStatus.COMPLETED);
        session.setResult(result);

        // Get player IDs
        Integer whitePlayerId = playerIds.get(session.getWhitePlayer());
        Integer blackPlayerId = playerIds.get(session.getBlackPlayer());

        if (whitePlayerId != null && blackPlayerId != null) {
            // Save the game using PGNService
            boolean saved = pgnService.saveCompletedGame(
                    session.getMoveHistory(),
                    whitePlayerId,
                    blackPlayerId,
                    result
            );

            if (saved) {
                logger.info("Successfully saved completed game: {} with result: {}",
                        session.getGameId(), result);
            } else {
                logger.error("Failed to save completed game: {}", session.getGameId());
            }
        } else {
            logger.warn("Cannot save game {} - missing player IDs (white: {}, black: {})",
                    session.getGameId(), whitePlayerId, blackPlayerId);
        }

        // Clean up session
        cleanupSession(session);
    }

    /**
     * Clean up game session
     */
    private void cleanupSession(GameSession session) {
        activeSessions.remove(session.getGameId());
        if (session.getWhitePlayer() != null) {
            playerToGameMap.remove(session.getWhitePlayer());
            playerIds.remove(session.getWhitePlayer());
        }
        if (session.getBlackPlayer() != null) {
            playerToGameMap.remove(session.getBlackPlayer());
            playerIds.remove(session.getBlackPlayer());
        }
    }

    private Player findOrCreateAnonymousPlayer(String username) {
        try {
            // Check if player already exists (shouldn't happen with timestamps, but just in case)
            Optional<Player> existingPlayer = playerRepository.findByUsername(username);
            if (existingPlayer.isPresent()) {
                logger.debug("Found existing player: {}", username);
                return existingPlayer.get();
            }

            // Create new anonymous player
            String email = username.toLowerCase() + "@anonymous.chess";
            String password = "anonymous_" + random.nextInt(10000);

            logger.debug("Creating new player: {} with email: {}", username, email);

            boolean created = playerRepository.save(username, email, password);
            if (created) {
                // Small delay to ensure database commit
                Thread.sleep(100);

                // Retrieve the created player
                Optional<Player> newPlayer = playerRepository.findByUsername(username);
                if (newPlayer.isPresent()) {
                    logger.info("Successfully created anonymous player: {} with ID: {}",
                            username, newPlayer.get().id());
                    return newPlayer.get();
                } else {
                    logger.error("Player created but not found in database: {}", username);
                }
            } else {
                logger.error("Failed to save player to database: {}", username);
            }

            return null;

        } catch (Exception e) {
            logger.error("Error creating anonymous player: {}", username, e);
            return null;
        }
    }

    /**
     * Determines if a move message indicates game end
     */
    private boolean isGameEndingMove(String moveMessage) {
        // Simple heuristic - in a real implementation, you'd have proper chess engine integration
        return moveMessage.contains("CHECKMATE") ||
                moveMessage.contains("RESIGNATION") ||
                moveMessage.contains("DRAW") ||
                moveMessage.contains("STALEMATE");
    }

    /**
     * Determines the game result from a move message
     */
    private String determineGameResult(String moveMessage) {
        if (moveMessage.contains("CHECKMATE")) {
            return moveMessage.contains("WHITE") ? "1-0" : "0-1";
        } else if (moveMessage.contains("RESIGNATION")) {
            return moveMessage.contains("WHITE") ? "0-1" : "1-0";
        } else if (moveMessage.contains("DRAW") || moveMessage.contains("STALEMATE")) {
            return "1/2-1/2";
        }
        return "*"; // Game in progress
    }

    /**
     * Force end a game (for testing or admin purposes)
     */
    public void forceEndGame(String gameId, String result) {
        GameSession session = activeSessions.get(gameId);
        if (session != null) {
            completeGame(session, result + " (forced)");
        }
    }

    /**
     * Gets the active game session for a player
     */
    public GameSession getPlayerGameSession(SocketChannel player) {
        String gameId = playerToGameMap.get(player);
        return gameId != null ? activeSessions.get(gameId) : null;
    }

    /**
     * Gets all active sessions (for monitoring/debugging)
     */
    public Map<String, GameSession> getActiveSessions() {
        return new ConcurrentHashMap<>(activeSessions);
    }

    /**
     * Get session statistics
     */
    public String getSessionStats() {
        int activeSessions = this.activeSessions.size();
        int connectedPlayers = playerToGameMap.size();
        return String.format("Active sessions: %d, Connected players: %d", activeSessions, connectedPlayers);
    }
}