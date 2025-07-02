package io.gchape.github.cli.config;

import io.gchape.github.model.entity.db.Game;
import io.gchape.github.model.service.PgnService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@ShellComponent
public class PgnCommands {
    private final PgnService pgnService;

    public PgnCommands(PgnService pgnService) {
        this.pgnService = pgnService;
    }

    /**
     * Export a single game to PGN file
     * Usage: export-game --game-id 1 --output-file game1.pgn
     */
    @ShellMethod(value = "Export a single game to PGN file", key = "export-game")
    public String exportGame(
            @ShellOption(value = {"--game-id", "-g"}) int gameId,
            @ShellOption(value = {"--output-file", "-o"}, defaultValue = "") String outputFile) {

        try {
            String pgnContent = pgnService.exportGameToPgn(gameId);

            if (outputFile.isEmpty()) {
                outputFile = "game_" + gameId + ".pgn";
            }

            pgnService.savePgnToFile(pgnContent, outputFile);
            return "Game " + gameId + " exported to " + outputFile;

        } catch (IllegalArgumentException e) {
            return "Error: Game not found with ID " + gameId;
        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Export all games by a player to PGN file
     * Usage: export-player-games --player-id 1 --output-file player1_games.pgn
     */
    @ShellMethod(value = "Export all games by a player to PGN file", key = "export-player-games")
    public String exportPlayerGames(
            @ShellOption(value = {"--player-id", "-p"}) int playerId,
            @ShellOption(value = {"--output-file", "-o"}, defaultValue = "") String outputFile) {

        try {
            String pgnContent = pgnService.exportPlayerGamesToPgn(playerId);

            if (pgnContent.trim().isEmpty()) {
                return "No games found for player ID " + playerId;
            }

            if (outputFile.isEmpty()) {
                outputFile = "player_" + playerId + "_games.pgn";
            }

            pgnService.savePgnToFile(pgnContent, outputFile);
            return "Player " + playerId + " games exported to " + outputFile;

        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Export multiple games to PGN file
     * Usage: export-games --game-ids 1,2,3,4 --output-file games.pgn
     */
    @ShellMethod(value = "Export multiple games to PGN file", key = "export-games")
    public String exportGames(
            @ShellOption(value = {"--game-ids", "-g"}) String gameIds,
            @ShellOption(value = {"--output-file", "-o"}, defaultValue = "") String outputFile) {

        try {
            List<Integer> ids = parseGameIds(gameIds);
            if (ids.isEmpty()) {
                return "Error: No valid game IDs provided";
            }

            String pgnContent = pgnService.exportGamesToPgn(ids);

            if (outputFile.isEmpty()) {
                outputFile = "games_export.pgn";
            }

            pgnService.savePgnToFile(pgnContent, outputFile);
            return ids.size() + " games exported to " + outputFile;

        } catch (IOException e) {
            return "Error writing file: " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Import games from PGN file
     * Usage: import-pgn --file games.pgn --white-player 1 --black-player 2
     */
    @ShellMethod(value = "Import games from PGN file", key = "import-pgn")
    public String importPgn(
            @ShellOption(value = {"--file", "-f"}) String filePath,
            @ShellOption(value = {"--white-player", "-w"}, defaultValue = "1") int whitePlayerId,
            @ShellOption(value = {"--black-player", "-b"}, defaultValue = "2") int blackPlayerId) {

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "Error: File not found: " + filePath;
            }

            if (!filePath.toLowerCase().endsWith(".pgn")) {
                return "Error: File must have .pgn extension";
            }

            List<Game> importedGames = pgnService.importPgnToDatabase(filePath, whitePlayerId, blackPlayerId);

            return "Successfully imported " + importedGames.size() + " games from " + filePath;

        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        } catch (Exception e) {
            return "Error importing PGN: " + e.getMessage();
        }
    }

    /**
     * Validate PGN file without importing
     * Usage: validate-pgn --file games.pgn
     */
    @ShellMethod(value = "Validate PGN file without importing", key = "validate-pgn")
    public String validatePgn(@ShellOption(value = {"--file", "-f"}) String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "Error: File not found: " + filePath;
            }

            List<PgnService.ParsedPgnGame> parsedGames = pgnService.loadPgnFromFile(filePath);

            return "✓ PGN file is valid and contains " + parsedGames.size() + " games";

        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        } catch (Exception e) {
            return "✗ Invalid PGN file: " + e.getMessage();
        }
    }

    /**
     * Preview PGN content of a game
     * Usage: preview-pgn --game-id 1
     */
    @ShellMethod(value = "Preview PGN content of a game", key = "preview-pgn")
    public String previewPgn(@ShellOption(value = {"--game-id", "-g"}) int gameId) {
        try {
            String pgnContent = pgnService.exportGameToPgn(gameId);
            return "PGN for Game " + gameId + ":\n" + pgnContent;

        } catch (IllegalArgumentException e) {
            return "Error: Game not found with ID " + gameId;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Show PGN export/import help
     * Usage: pgn-help
     */
    @ShellMethod(value = "Show PGN commands help", key = "pgn-help")
    public String pgnHelp() {
        return """
                PGN Commands Help:
                            
                EXPORT COMMANDS:
                • export-game -g <gameId> -o <file>        Export single game
                • export-games -g <id1,id2,id3> -o <file>  Export multiple games
                • export-player-games -p <playerId> -o <file> Export all player games
                            
                IMPORT COMMANDS:
                • import-pgn -f <file.pgn> -w <whiteId> -b <blackId>  Import games from PGN
                • validate-pgn -f <file.pgn>               Validate PGN file
                            
                UTILITY COMMANDS:
                • preview-pgn -g <gameId>                  Preview game as PGN
                • pgn-help                                 Show this help
                            
                EXAMPLES:
                • export-game --game-id 1 --output-file my_game.pgn
                • import-pgn --file downloaded_games.pgn --white-player 3 --black-player 4
                • export-player-games --player-id 2 --output-file all_my_games.pgn
                """;
    }

    // Helper methods
    private List<Integer> parseGameIds(String gameIds) {
        try {
            return List.of(gameIds.split(","))
                    .stream()
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }
}
