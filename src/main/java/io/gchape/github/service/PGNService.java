package io.gchape.github.service;

import java.io.*;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class PGNService {
    private final DatabaseService databaseService;

    public PGNService() {
        this.databaseService = DatabaseService.getInstance(); // Use singleton
    }

    public PGNService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public void exportGameToPGN(Long gameId, String filePath) throws SQLException, IOException {
        String pgn = databaseService.generatePGN(gameId);

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(pgn);
        }
    }

    public void exportAllGamesToPGN(String directoryPath) throws SQLException, IOException {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Get all games and export each one
        List<Long> gameIds = databaseService.getAllGameIds();

        for (Long gameId : gameIds) {
            String fileName = "game_" + gameId + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pgn";
            String filePath = directoryPath + File.separator + fileName;

            try {
                exportGameToPGN(gameId, filePath);
            } catch (Exception e) {
                System.err.println("Failed to export game " + gameId + ": " + e.getMessage());
            }
        }
    }

    public List<String> importPGNFile(String filePath, Long importedBy) throws IOException, SQLException {
        List<String> importedGames = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder pgnContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                pgnContent.append(line).append("\n");
            }

            // Parse PGN and create games in database
            // This is a complex parsing task - simplified for now
            String content = pgnContent.toString();

            // Store PGN file record
            databaseService.storePGNFile(filePath, content, importedBy);

            importedGames.add("Game imported from " + filePath);
        }

        return importedGames;
    }
}