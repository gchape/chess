package io.gchape.github.cli.config;

import io.gchape.github.model.service.PGNTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class PGNTestCommands {

    private final PGNTestService pgnTestService;

    @Autowired
    public PGNTestCommands(PGNTestService pgnTestService) {
        this.pgnTestService = pgnTestService;
    }

    @ShellMethod(value = "Run comprehensive PGN functionality tests", key = "test-pgn")
    public String testPGN() {
        try {
            pgnTestService.runPGNTests();
            return "✅ PGN tests completed. Check logs for detailed results.";
        } catch (Exception e) {
            return "❌ Error running PGN tests: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Create a sample game for testing", key = "create-sample-game")
    public String createSampleGame() {
        try {
            var game = pgnTestService.createSampleGame();
            if (game != null) {
                return "✅ Sample game created with ID: " + game.id();
            } else {
                return "❌ Failed to create sample game";
            }
        } catch (Exception e) {
            return "❌ Error creating sample game: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Test PGN export for a specific game", key = "test-export")
    public String testExport(@ShellOption(value = "--game-id", help = "Game ID to export") int gameId) {
        try {
            boolean success = pgnTestService.testPGNExport(gameId);
            if (success) {
                return "✅ PGN export test passed for game ID: " + gameId;
            } else {
                return "❌ PGN export test failed for game ID: " + gameId;
            }
        } catch (Exception e) {
            return "❌ Error testing PGN export: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Test PGN import from file", key = "test-import")
    public String testImport(@ShellOption(value = "--file", help = "PGN file path to import") String filePath) {
        try {
            boolean success = pgnTestService.testPGNImport(filePath);
            if (success) {
                return "✅ PGN import test passed for file: " + filePath;
            } else {
                return "❌ PGN import test failed for file: " + filePath;
            }
        } catch (Exception e) {
            return "❌ Error testing PGN import: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Create a sample PGN file for testing", key = "create-sample-pgn")
    public String createSamplePGN(@ShellOption(value = "--file", help = "Output file path") String filePath) {
        try {
            String createdFile = pgnTestService.createSamplePGNFile(filePath);
            if (createdFile != null) {
                return "✅ Sample PGN file created: " + createdFile;
            } else {
                return "❌ Failed to create sample PGN file";
            }
        } catch (Exception e) {
            return "❌ Error creating sample PGN file: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Show game statistics and validate gameplay format", key = "game-stats")
    public String gameStats() {
        try {
            pgnTestService.printGameStatistics();
            return "✅ Game statistics printed to logs";
        } catch (Exception e) {
            return "❌ Error getting game statistics: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Show PGN testing help", key = "pgn-test-help")
    public String pgnTestHelp() {
        return """
                🧪 PGN Testing Commands Help:
                
                🔹 Run all PGN tests:
                   test-pgn
                   
                🔹 Create sample game:
                   create-sample-game
                   
                🔹 Test PGN export:
                   test-export --game-id <ID>
                   Example: test-export --game-id 1
                   
                🔹 Test PGN import:
                   test-import --file <path>
                   Example: test-import --file sample.pgn
                   
                🔹 Create sample PGN file:
                   create-sample-pgn --file <path>
                   Example: create-sample-pgn --file test.pgn
                   
                🔹 Show game statistics:
                   game-stats
                   
                📝 Testing workflow:
                1. create-sample-game (creates test data)
                2. test-pgn (runs comprehensive tests)
                3. game-stats (validates data format)
                
                💡 The test suite will:
                - Create sample games with valid move sequences
                - Test PGN export to string and file
                - Create sample PGN files for import testing
                - Import PGN files and validate the process
                """;
    }
}