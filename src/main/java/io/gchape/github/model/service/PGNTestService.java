package io.gchape.github.model.service;

import io.gchape.github.model.entity.db.Game;
import io.gchape.github.model.entity.db.Player;
import io.gchape.github.model.repository.GameRepository;
import io.gchape.github.model.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service to help test and validate PGN functionality
 */
@Service
public class PGNTestService {
    private static final Logger logger = LoggerFactory.getLogger(PGNTestService.class);

    private final PGNService pgnService;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public PGNTestService(PGNService pgnService, GameRepository gameRepository, PlayerRepository playerRepository) {
        this.pgnService = pgnService;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Create a sample game for testing PGN export
     */
    public Game createSampleGame() {
        try {
            // Create sample players if they don't exist
            int whitePlayerId = findOrCreatePlayer("TestWhite", "white@test.com");
            int blackPlayerId = findOrCreatePlayer("TestBlack", "black@test.com");

            if (whitePlayerId == -1 || blackPlayerId == -1) {
                logger.error("Failed to create sample players");
                return null;
            }

            // Create sample gameplay (simple opening moves)
            String sampleGameplay = "(6,4)->(4,4) (1,4)->(3,4) (6,3)->(4,3) (1,3)->(3,3) (7,1)->(5,2) (0,1)->(2,2)";

            Game sampleGame = new Game(
                    0, // ID will be generated
                    whitePlayerId,
                    blackPlayerId,
                    Instant.now(),
                    sampleGameplay
            );

            Optional<Game> savedGame = gameRepository.save(sampleGame);
            if (savedGame.isPresent()) {
                logger.info("Created sample game with ID: {}", savedGame.get().id());
                return savedGame.get();
            } else {
                logger.error("Failed to save sample game");
                return null;
            }

        } catch (Exception e) {
            logger.error("Error creating sample game", e);
            return null;
        }
    }

    /**
     * Test PGN export functionality
     */
    public boolean testPGNExport(int gameId) {
        try {
            String pgn = pgnService.exportGameToPGN(gameId);
            if (pgn != null) {
                logger.info("Successfully exported game {} to PGN", gameId);
                logger.info("PGN content:\n{}", pgn);
                return true;
            } else {
                logger.error("Failed to export game {} to PGN", gameId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error testing PGN export", e);
            return false;
        }
    }

    /**
     * Test PGN file export
     */
    public boolean testPGNFileExport(List<Integer> gameIds, String filePath) {
        try {
            boolean success = pgnService.exportGamesToFile(gameIds, filePath);
            if (success) {
                logger.info("Successfully exported {} games to file: {}", gameIds.size(), filePath);
                return true;
            } else {
                logger.error("Failed to export games to file: {}", filePath);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error testing PGN file export", e);
            return false;
        }
    }

    /**
     * Test PGN import functionality
     */
    public boolean testPGNImport(String filePath) {
        try {
            int importedCount = pgnService.importGamesFromFile(filePath);
            if (importedCount > 0) {
                logger.info("Successfully imported {} games from file: {}", importedCount, filePath);
                return true;
            } else {
                logger.error("No games were imported from file: {}", filePath);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error testing PGN import", e);
            return false;
        }
    }

    /**
     * Create a sample PGN file for testing import
     */
    public String createSamplePGNFile(String filePath) {
        try {
            String samplePGN = """
                [Event "Test Game 1"]
                [Site "Test Application"]
                [Date "2025.01.15"]
                [Round "1"]
                [White "AliceTest"]
                [Black "BobTest"]
                [Result "*"]
                
                1.e4 e5 2.Nf3 Nc6 3.Bb5 a6 *
                
                [Event "Test Game 2"]
                [Site "Test Application"]
                [Date "2025.01.15"]
                [Round "1"]
                [White "CharlieTest"]
                [Black "DaveTest"]
                [Result "*"]
                
                1.d4 d5 2.c4 e6 3.Nc3 Nf6 *
                """;

            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), samplePGN.getBytes());
            logger.info("Created sample PGN file: {}", filePath);
            return filePath;

        } catch (Exception e) {
            logger.error("Error creating sample PGN file", e);
            return null;
        }
    }

    /**
     * Run comprehensive PGN tests
     */
    public void runPGNTests() {
        logger.info("Starting PGN functionality tests...");

        // Test 1: Create sample game
        Game sampleGame = createSampleGame();
        if (sampleGame == null) {
            logger.error("Failed to create sample game. Aborting tests.");
            return;
        }

        // Test 2: Export to PGN string
        boolean exportSuccess = testPGNExport(sampleGame.id());
        if (!exportSuccess) {
            logger.error("PGN export test failed");
        }

        // Test 3: Export to file
        String exportFile = "test_export.pgn";
        boolean fileExportSuccess = testPGNFileExport(List.of(sampleGame.id()), exportFile);
        if (!fileExportSuccess) {
            logger.error("PGN file export test failed");
        }

        // Test 4: Create sample PGN for import
        String importFile = "test_import.pgn";
        String sampleFile = createSamplePGNFile(importFile);
        if (sampleFile == null) {
            logger.error("Failed to create sample PGN file");
        }

        // Test 5: Import PGN file
        if (sampleFile != null) {
            boolean importSuccess = testPGNImport(importFile);
            if (!importSuccess) {
                logger.error("PGN import test failed");
            }
        }

        logger.info("PGN functionality tests completed");
    }

    /**
     * Find or create a player for testing
     */
    private int findOrCreatePlayer(String username, String email) {
        Optional<Player> existingPlayer = playerRepository.findByUsername(username);
        if (existingPlayer.isPresent()) {
            return existingPlayer.get().id();
        }

        boolean created = playerRepository.save(username, email, "testpassword123");
        if (created) {
            Optional<Player> newPlayer = playerRepository.findByUsername(username);
            return newPlayer.map(Player::id).orElse(-1);
        }

        return -1;
    }

    /**
     * Validate gameplay string format
     */
    public boolean validateGameplayFormat(String gameplay) {
        if (gameplay == null || gameplay.trim().isEmpty()) {
            return false;
        }

        // Check if it matches the expected format: (row,col)->(row,col)
        String pattern = "\\(\\d+,\\d+\\)->\\(\\d+,\\d+\\)";
        String[] moves = gameplay.trim().split("\\s+");

        for (String move : moves) {
            if (!move.matches(pattern)) {
                logger.warn("Invalid move format: {}", move);
                return false;
            }
        }

        return true;
    }

    /**
     * Get statistics about games in database
     */
    public void printGameStatistics() {
        try {
            List<Game> allGames = gameRepository.findAll();
            logger.info("=== Game Statistics ===");
            logger.info("Total games: {}", allGames.size());

            int validGameplay = 0;
            int invalidGameplay = 0;

            for (Game game : allGames) {
                if (validateGameplayFormat(game.gameplay())) {
                    validGameplay++;
                } else {
                    invalidGameplay++;
                    logger.warn("Game {} has invalid gameplay format: {}", game.id(), game.gameplay());
                }
            }

            logger.info("Games with valid gameplay format: {}", validGameplay);
            logger.info("Games with invalid gameplay format: {}", invalidGameplay);

        } catch (Exception e) {
            logger.error("Error getting game statistics", e);
        }
    }
}