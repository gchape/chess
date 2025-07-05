package io.gchape.github.cli.config;

import io.gchape.github.model.entity.db.Game;
import io.gchape.github.model.repository.GameRepository;
import io.gchape.github.model.service.PGNService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ShellComponent
public class PGNCommands {

    private final PGNService pgnService;
    private final GameRepository gameRepository;

    @Autowired
    public PGNCommands(PGNService pgnService, GameRepository gameRepository) {
        this.pgnService = pgnService;
        this.gameRepository = gameRepository;
    }

    @ShellMethod(value = "Export a single game to PGN format", key = "export-game")
    public String exportGame(@ShellOption(value = "--game-id", help = "ID of the game to export") int gameId,
                             @ShellOption(value = "--file", help = "Output file path (optional)") String filePath) {
        try {
            String pgn = pgnService.exportGameToPGN(gameId);

            if (pgn == null) {
                return "❌ Game not found with ID: " + gameId;
            }

            if (filePath != null && !filePath.trim().isEmpty()) {
                // Save to file
                boolean success = pgnService.exportGamesToFile(List.of(gameId), filePath);
                if (success) {
                    return "✅ Game exported successfully to: " + filePath;
                } else {
                    return "❌ Failed to export game to file: " + filePath;
                }
            } else {
                // Return PGN string
                return "🎯 Game " + gameId + " in PGN format:\n\n" + pgn;
            }

        } catch (Exception e) {
            return "❌ Error exporting game: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Export multiple games to PGN file", key = "export-games")
    public String exportGames(@ShellOption(value = "--game-ids", help = "Comma-separated list of game IDs") String gameIds,
                              @ShellOption(value = "--file", help = "Output file path") String filePath) {
        try {
            List<Integer> ids = Arrays.stream(gameIds.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            boolean success = pgnService.exportGamesToFile(ids, filePath);

            if (success) {
                return "✅ Successfully exported " + ids.size() + " games to: " + filePath;
            } else {
                return "❌ Failed to export games to file: " + filePath;
            }

        } catch (NumberFormatException e) {
            return "❌ Invalid game IDs format. Use comma-separated numbers (e.g., 1,2,3)";
        } catch (Exception e) {
            return "❌ Error exporting games: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Export all games to PGN file", key = "export-all-games")
    public String exportAllGames(@ShellOption(value = "--file", help = "Output file path") String filePath) {
        try {
            boolean success = pgnService.exportAllGamesToFile(filePath);

            if (success) {
                List<Game> games = gameRepository.findAll();
                return "✅ Successfully exported " + games.size() + " games to: " + filePath;
            } else {
                return "❌ Failed to export games to file: " + filePath;
            }

        } catch (Exception e) {
            return "❌ Error exporting all games: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Import games from PGN file", key = "import-games")
    public String importGames(@ShellOption(value = "--file", help = "Path to PGN file") String filePath) {
        try {
            // Check if file exists
            File file = new File(filePath);
            if (!file.exists()) {
                return "❌ File not found: " + filePath;
            }

            if (!file.canRead()) {
                return "❌ Cannot read file: " + filePath;
            }

            int importedCount = pgnService.importGamesFromFile(filePath);

            if (importedCount > 0) {
                return "✅ Successfully imported " + importedCount + " games from: " + filePath;
            } else {
                return "❌ No games were imported from: " + filePath + ". Check file format and content.";
            }

        } catch (Exception e) {
            return "❌ Error importing games: " + e.getMessage();
        }
    }

    @ShellMethod(value = "List all games in database", key = "list-games")
    public String listGames() {
        try {
            List<Game> games = gameRepository.findAll();

            if (games.isEmpty()) {
                return "📋 No games found in database.";
            }

            StringBuilder result = new StringBuilder("📋 Games in database:\n\n");
            for (Game game : games) {
                result.append("ID: ").append(game.id())
                        .append(" | White: ").append(game.playerWhiteId())
                        .append(" | Black: ").append(game.playerBlackId())
                        .append(" | Date: ").append(game.startTime())
                        .append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "❌ Error listing games: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Show game details", key = "show-game")
    public String showGame(@ShellOption(value = "--game-id", help = "ID of the game to show") int gameId) {
        try {
            var gameOpt = gameRepository.findById(gameId);

            if (gameOpt.isEmpty()) {
                return "❌ Game not found with ID: " + gameId;
            }

            Game game = gameOpt.get();
            StringBuilder result = new StringBuilder("🎯 Game Details:\n\n");
            result.append("ID: ").append(game.id()).append("\n");
            result.append("White Player ID: ").append(game.playerWhiteId()).append("\n");
            result.append("Black Player ID: ").append(game.playerBlackId()).append("\n");
            result.append("Start Time: ").append(game.startTime()).append("\n");
            result.append("Gameplay: ").append(game.gameplay()).append("\n");

            return result.toString();

        } catch (Exception e) {
            return "❌ Error showing game: " + e.getMessage();
        }
    }

    @ShellMethod(value = "Show PGN export help", key = "pgn-help")
    public String pgnHelp() {
        return """
                📚 PGN Export/Import Commands Help:
                
                🔹 Export single game:
                   export-game --game-id <ID> [--file <path>]
                   Example: export-game --game-id 1 --file /path/to/game.pgn
                
                🔹 Export multiple games:
                   export-games --game-ids <ID1,ID2,ID3> --file <path>
                   Example: export-games --game-ids 1,2,3 --file /path/to/games.pgn
                
                🔹 Export all games:
                   export-all-games --file <path>
                   Example: export-all-games --file /path/to/all_games.pgn
                
                🔹 Import games from PGN:
                   import-games --file <path>
                   Example: import-games --file /path/to/import.pgn
                
                🔹 List games:
                   list-games
                
                🔹 Show game details:
                   show-game --game-id <ID>
                   Example: show-game --game-id 1
                
                📝 Notes:
                - PGN files use standard Portable Game Notation format
                - Imported players get default credentials (username@imported.chess / imported123)
                - Games are exported with player usernames and game timestamps
                - If no file path is specified for single game export, PGN is displayed in console
                """;
    }
}