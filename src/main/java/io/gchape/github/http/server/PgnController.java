package io.gchape.github.http.server;

import io.gchape.github.model.entity.db.Game;
import io.gchape.github.model.service.PgnService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pgn")
public class PgnController {
    private final PgnService pgnService;

    public PgnController(PgnService pgnService) {
        this.pgnService = pgnService;
    }

    /**
     * Export a single game as PGN
     * GET /api/pgn/export/game/{gameId}
     */
    @GetMapping("/export/game/{gameId}")
    public ResponseEntity<String> exportSingleGame(@PathVariable int gameId) {
        try {
            String pgnContent = pgnService.exportGameToPgn(gameId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "game_" + gameId + ".pgn");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pgnContent);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting game: " + e.getMessage());
        }
    }

    /**
     * Export multiple games as PGN
     * POST /api/pgn/export/games
     * Body: {"gameIds": [1, 2, 3]}
     */
    @PostMapping("/export/games")
    public ResponseEntity<String> exportMultipleGames(@RequestBody Map<String, List<Integer>> request) {
        try {
            List<Integer> gameIds = request.get("gameIds");
            if (gameIds == null || gameIds.isEmpty()) {
                return ResponseEntity.badRequest().body("No game IDs provided");
            }

            String pgnContent = pgnService.exportGamesToPgn(gameIds);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            String filename = "games_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pgn";
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pgnContent);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting games: " + e.getMessage());
        }
    }

    /**
     * Export all games by a player as PGN
     * GET /api/pgn/export/player/{playerId}
     */
    @GetMapping("/export/player/{playerId}")
    public ResponseEntity<String> exportPlayerGames(@PathVariable int playerId) {
        try {
            String pgnContent = pgnService.exportPlayerGamesToPgn(playerId);

            if (pgnContent.trim().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            String filename = "player_" + playerId + "_games.pgn";
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pgnContent);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error exporting player games: " + e.getMessage());
        }
    }

    /**
     * Import PGN file and save games to database
     * POST /api/pgn/import
     * Form data: file, whitePlayerId (optional), blackPlayerId (optional)
     */
    @PostMapping("/import")
    public ResponseEntity<?> importPgnFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "whitePlayerId", defaultValue = "1") int whitePlayerId,
            @RequestParam(value = "blackPlayerId", defaultValue = "2") int blackPlayerId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".pgn")) {
            return ResponseEntity.badRequest().body("File must be a .pgn file");
        }

        try {
            // Save uploaded file temporarily
            Path tempFile = Files.createTempFile("chess_import_", ".pgn");
            file.transferTo(tempFile.toFile());

            // Import games
            List<Game> importedGames = pgnService.importPgnToDatabase(
                    tempFile.toString(),
                    whitePlayerId,
                    blackPlayerId
            );

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully imported " + importedGames.size() + " games",
                    "importedGames", importedGames.size(),
                    "gameIds", importedGames.stream().map(Game::id).toList()
            ));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error importing PGN: " + e.getMessage());
        }
    }

    /**
     * Import PGN from file path (for server-side files)
     * POST /api/pgn/import/file
     * Body: {"filePath": "/path/to/file.pgn", "whitePlayerId": 1, "blackPlayerId": 2}
     */
    @PostMapping("/import/file")
    public ResponseEntity<?> importPgnFromPath(@RequestBody Map<String, Object> request) {
        try {
            String filePath = (String) request.get("filePath");
            int whitePlayerId = (Integer) request.getOrDefault("whitePlayerId", 1);
            int blackPlayerId = (Integer) request.getOrDefault("blackPlayerId", 2);

            if (filePath == null || filePath.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("File path is required");
            }

            List<Game> importedGames = pgnService.importPgnToDatabase(filePath, whitePlayerId, blackPlayerId);

            return ResponseEntity.ok(Map.of(
                    "message", "Successfully imported " + importedGames.size() + " games",
                    "importedGames", importedGames.size(),
                    "gameIds", importedGames.stream().map(Game::id).toList()
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error importing PGN: " + e.getMessage());
        }
    }

    /**
     * Get PGN content for preview (without downloading)
     * GET /api/pgn/preview/game/{gameId}
     */
    @GetMapping("/preview/game/{gameId}")
    public ResponseEntity<Map<String, String>> previewGamePgn(@PathVariable int gameId) {
        try {
            String pgnContent = pgnService.exportGameToPgn(gameId);
            return ResponseEntity.ok(Map.of("pgn", pgnContent));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Validate PGN file without importing
     * POST /api/pgn/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validatePgnFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }

        try {
            // Save uploaded file temporarily
            Path tempFile = Files.createTempFile("chess_validate_", ".pgn");
            file.transferTo(tempFile.toFile());

            // Parse but don't save to database
            List<PgnService.ParsedPgnGame> parsedGames = pgnService.loadPgnFromFile(tempFile.toString());

            // Clean up temp file
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "gameCount", parsedGames.size(),
                    "message", "PGN file is valid and contains " + parsedGames.size() + " games"
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }
}