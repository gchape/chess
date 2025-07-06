package io.gchape.github.model.service;

import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import io.gchape.github.model.entity.db.Game;
import io.gchape.github.model.entity.db.Player;
import io.gchape.github.model.repository.GameRepository;
import io.gchape.github.model.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced PGNService that handles saving live games when they complete
 */
@Service
public class PGNService {
    private static final Logger logger = LoggerFactory.getLogger(PGNService.class);
    private static final Pattern NETWORK_MOVE_PATTERN = Pattern.compile("([A-Za-z]+)#\\((\\d+),(\\d+)\\)->\\((\\d+),(\\d+)\\)");
    private static final Pattern COORDINATE_MOVE_PATTERN = Pattern.compile("\\((\\d+),(\\d+)\\)->\\((\\d+),(\\d+)\\)");
    private static final DateTimeFormatter PGN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    @Autowired
    public PGNService(GameRepository gameRepository, PlayerRepository playerRepository) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Saves a completed live game to the database
     */
    public boolean saveCompletedGame(List<String> moveHistory, int whitePlayerId, int blackPlayerId, String result) {
        try {
            // Convert move history to the format expected by the database
            String gameplay = convertMovesToGameplayFormat(moveHistory);

            if (gameplay.isEmpty()) {
                logger.warn("No valid moves found in move history for game between players {} and {}",
                        whitePlayerId, blackPlayerId);
                // Still save the game even if no moves (for disconnection cases)
                gameplay = "";
            }

            // Create game entity
            Game game = new Game(
                    0, // ID will be generated
                    whitePlayerId,
                    blackPlayerId,
                    Instant.now(),
                    gameplay
            );

            // Save to database
            Optional<Game> savedGame = gameRepository.save(game);

            if (savedGame.isPresent()) {
                logger.info("Successfully saved completed game with ID: {} for players {} vs {} (Result: {})",
                        savedGame.get().id(), whitePlayerId, blackPlayerId, result);

                // Optionally, also export to PGN format for backup
                exportCompletedGameToPGN(savedGame.get(), result);

                return true;
            } else {
                logger.error("Failed to save game to database");
                return false;
            }

        } catch (Exception e) {
            logger.error("Error saving completed game for players {} vs {}", whitePlayerId, blackPlayerId, e);
            return false;
        }
    }

    /**
     * Converts move history from network format to database format
     */
    private String convertMovesToGameplayFormat(List<String> moveHistory) {
        StringBuilder gameplay = new StringBuilder();

        for (String move : moveHistory) {
            if (isValidMoveMessage(move)) {
                String convertedMove = convertMoveMessage(move);
                if (!convertedMove.isEmpty()) {
                    if (!gameplay.isEmpty()) {
                        gameplay.append(" ");
                    }
                    gameplay.append(convertedMove);
                }
            }
        }

        return gameplay.toString();
    }

    /**
     * Validates if a message is a valid move message
     */
    private boolean isValidMoveMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }

        // Skip special game messages
        if (message.contains("CHECKMATE") || message.contains("RESIGNATION") ||
                message.contains("DRAW") || message.contains("STALEMATE")) {
            return false;
        }

        return NETWORK_MOVE_PATTERN.matcher(message).matches();
    }

    /**
     * Converts a single move message from network format to database format
     */
    private String convertMoveMessage(String moveMessage) {
        Matcher matcher = NETWORK_MOVE_PATTERN.matcher(moveMessage);

        if (matcher.matches()) {
            // Extract coordinates from network format: PIECE#(fromRow,fromCol)->(toRow,toCol)
            int fromRow = Integer.parseInt(matcher.group(2));
            int fromCol = Integer.parseInt(matcher.group(3));
            int toRow = Integer.parseInt(matcher.group(4));
            int toCol = Integer.parseInt(matcher.group(5));

            // Convert to database format: (fromRow,fromCol)->(toRow,toCol)
            return String.format("(%d,%d)->(%d,%d)", fromRow, fromCol, toRow, toCol);
        }

        logger.warn("Could not parse move message: {}", moveMessage);
        return "";
    }

    /**
     * Exports a completed game to PGN format for backup
     */
    private void exportCompletedGameToPGN(Game game, String result) {
        try {
            String pgnContent = exportGameToPGN(game.id());
            if (pgnContent != null) {
                logger.debug("Generated PGN for completed game {}: {}", game.id(), pgnContent);
                // Could save to file or store in another table if needed
            }
        } catch (Exception e) {
            logger.warn("Could not export completed game {} to PGN", game.id(), e);
        }
    }

    /**
     * Handles immediate game ending (resignation, abandonment)
     */
    public boolean saveGameWithResult(List<String> moveHistory, int whitePlayerId, int blackPlayerId,
                                      String result, String endReason) {
        try {
            boolean saved = saveCompletedGame(moveHistory, whitePlayerId, blackPlayerId, result);

            if (saved) {
                logger.info("Game ended: {} - Result: {} ({})",
                        getPlayerNames(whitePlayerId, blackPlayerId), result, endReason);
            }

            return saved;

        } catch (Exception e) {
            logger.error("Error saving game with result: {}", result, e);
            return false;
        }
    }

    /**
     * Gets player names for logging
     */
    private String getPlayerNames(int whitePlayerId, int blackPlayerId) {
        try {
            Optional<Player> whitePlayer = playerRepository.findById(whitePlayerId);
            Optional<Player> blackPlayer = playerRepository.findById(blackPlayerId);

            String whiteName = whitePlayer.map(Player::username).orElse("Unknown");
            String blackName = blackPlayer.map(Player::username).orElse("Unknown");

            return whiteName + " vs " + blackName;

        } catch (Exception e) {
            return "Player " + whitePlayerId + " vs Player " + blackPlayerId;
        }
    }

    /**
     * Export a single game to PGN format
     */
    public String exportGameToPGN(int gameId) {
        try {
            Optional<Game> gameOpt = gameRepository.findById(gameId);
            if (gameOpt.isEmpty()) {
                logger.warn("Game not found with ID: {}", gameId);
                return null;
            }

            Game game = gameOpt.get();
            Optional<Player> whitePlayer = playerRepository.findById(game.playerWhiteId());
            Optional<Player> blackPlayer = playerRepository.findById(game.playerBlackId());

            return buildPGNString(game, whitePlayer.orElse(null), blackPlayer.orElse(null));

        } catch (Exception e) {
            logger.error("Error exporting game {} to PGN", gameId, e);
            return null;
        }
    }

    /**
     * Export multiple games to PGN file
     */
    public boolean exportGamesToFile(List<Integer> gameIds, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (int i = 0; i < gameIds.size(); i++) {
                String pgn = exportGameToPGN(gameIds.get(i));
                if (pgn != null) {
                    writer.write(pgn);
                    if (i < gameIds.size() - 1) {
                        writer.write("\n\n");
                    }
                }
            }
            logger.info("Successfully exported {} games to {}", gameIds.size(), filePath);
            return true;
        } catch (IOException e) {
            logger.error("Error writing PGN file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Export all games to PGN file
     */
    public boolean exportAllGamesToFile(String filePath) {
        try {
            List<Game> games = gameRepository.findAll();
            List<Integer> gameIds = games.stream().map(Game::id).toList();
            return exportGamesToFile(gameIds, filePath);
        } catch (Exception e) {
            logger.error("Error exporting all games to PGN file", e);
            return false;
        }
    }

    /**
     * Import games from PGN file into database
     */
    public int importGamesFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            List<PGNGameData> games = parsePGNFile(reader);
            int importedCount = 0;

            for (PGNGameData gameData : games) {
                if (saveGameToDatabase(gameData)) {
                    importedCount++;
                }
            }

            logger.info("Successfully imported {} games from {}", importedCount, filePath);
            return importedCount;

        } catch (IOException e) {
            logger.error("Error reading PGN file: {}", filePath, e);
            return 0;
        } catch (Exception e) {
            logger.error("Error importing games from PGN file", e);
            return 0;
        }
    }

    /**
     * Build PGN string for a game
     */
    private String buildPGNString(Game game, Player whitePlayer, Player blackPlayer) {
        StringBuilder pgn = new StringBuilder();

        // PGN Headers
        pgn.append("[Event \"Chess Game\"]\n");
        pgn.append("[Site \"Chess Application\"]\n");
        pgn.append("[Date \"").append(game.startTime().atZone(ZoneId.systemDefault()).format(PGN_DATE_FORMAT)).append("\"]\n");
        pgn.append("[Round \"1\"]\n");
        pgn.append("[White \"").append(whitePlayer != null ? whitePlayer.username() : "Unknown").append("\"]\n");
        pgn.append("[Black \"").append(blackPlayer != null ? blackPlayer.username() : "Unknown").append("\"]\n");
        pgn.append("[Result \"*\"]\n");
        pgn.append("\n");

        // Convert moves to PGN format
        String moves = convertMovesToPGN(game.gameplay());
        pgn.append(moves);

        if (!moves.endsWith("*")) {
            pgn.append(" *");
        }

        return pgn.toString();
    }

    /**
     * Convert internal move format to PGN algebraic notation
     */
    private String convertMovesToPGN(String gameplay) {
        if (gameplay == null || gameplay.trim().isEmpty()) {
            return "*";
        }

        StringBuilder pgn = new StringBuilder();
        List<Move> moves = parseMovesFromGameplay(gameplay);

        // Initialize board state to track piece positions
        Piece[][] board = initializeBoard();

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);

            if (i % 2 == 0) {
                pgn.append((i / 2) + 1).append(".");
            }

            String algebraicMove = convertToAlgebraicNotation(move, board);
            pgn.append(algebraicMove);

            // Update board state
            updateBoard(board, move);

            if (i < moves.size() - 1) {
                pgn.append(" ");
            }
        }

        return pgn.toString();
    }

    /**
     * Parse moves from gameplay string (for PGN conversion)
     */
    private List<Move> parseMovesFromGameplay(String gameplay) {
        List<Move> moves = new ArrayList<>();

        if (gameplay == null || gameplay.trim().isEmpty()) {
            return moves;
        }

        Matcher matcher = COORDINATE_MOVE_PATTERN.matcher(gameplay);

        while (matcher.find()) {
            int fromRow = Integer.parseInt(matcher.group(1));
            int fromCol = Integer.parseInt(matcher.group(2));
            int toRow = Integer.parseInt(matcher.group(3));
            int toCol = Integer.parseInt(matcher.group(4));

            moves.add(new Move(new Position(fromRow, fromCol), new Position(toRow, toCol)));
        }

        return moves;
    }

    /**
     * Initialize standard chess board
     */
    private Piece[][] initializeBoard() {
        Piece[][] board = new Piece[8][8];

        // Initialize pawns
        for (int col = 0; col < 8; col++) {
            board[1][col] = Piece.BP;
            board[6][col] = Piece.WP;
        }

        // Initialize back pieces
        Piece[] backRow = {Piece.BR, Piece.BN, Piece.BB, Piece.BQ, Piece.BK, Piece.BB, Piece.BN, Piece.BR};
        Piece[] whiteRow = {Piece.WR, Piece.WN, Piece.WB, Piece.WQ, Piece.WK, Piece.WB, Piece.WN, Piece.WR};

        System.arraycopy(backRow, 0, board[0], 0, 8);
        System.arraycopy(whiteRow, 0, board[7], 0, 8);

        return board;
    }

    /**
     * Update board state after a move
     */
    private void updateBoard(Piece[][] board, Move move) {
        Piece piece = board[move.from().row()][move.from().col()];
        board[move.to().row()][move.to().col()] = piece;
        board[move.from().row()][move.from().col()] = null;
    }

    /**
     * Convert move to algebraic notation
     */
    private String convertToAlgebraicNotation(Move move, Piece[][] board) {
        Position from = move.from();
        Position to = move.to();
        Piece piece = board[from.row()][from.col()];

        if (piece == null) {
            return positionToAlgebraic(from) + positionToAlgebraic(to);
        }

        String toSquare = positionToAlgebraic(to);
        boolean isCapture = board[to.row()][to.col()] != null;

        // Handle pawn moves
        if (piece == Piece.WP || piece == Piece.BP) {
            if (isCapture) {
                return (char) ('a' + from.col()) + "x" + toSquare;
            } else {
                return toSquare;
            }
        }

        // Handle other pieces
        String pieceSymbol = getPieceSymbol(piece);
        String capture = isCapture ? "x" : "";

        return pieceSymbol + capture + toSquare;
    }

    /**
     * Get piece symbol for algebraic notation
     */
    private String getPieceSymbol(Piece piece) {
        return switch (piece) {
            case WK, BK -> "K";
            case WQ, BQ -> "Q";
            case WR, BR -> "R";
            case WB, BB -> "B";
            case WN, BN -> "N";
            default -> "";
        };
    }

    /**
     * Convert position to algebraic notation (e.g., (0,0) -> "a8")
     */
    private String positionToAlgebraic(Position pos) {
        char file = (char) ('a' + pos.col());
        int rank = 8 - pos.row();
        return "" + file + rank;
    }

    /**
     * Convert algebraic notation to position (e.g., "a8" -> (0,0))
     */
    private Position algebraicToPosition(String algebraic) {
        if (algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }

        char file = algebraic.charAt(0);
        char rank = algebraic.charAt(1);

        int col = file - 'a';
        int row = 8 - (rank - '0');

        return new Position(row, col);
    }

    /**
     * Parse PGN file and extract game data
     */
    private List<PGNGameData> parsePGNFile(BufferedReader reader) throws IOException {
        List<PGNGameData> games = new ArrayList<>();
        PGNGameData currentGame = null;
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("[")) {
                // Header line
                if (currentGame == null) {
                    currentGame = new PGNGameData();
                }
                parseHeader(line, currentGame);
            } else if (!line.isEmpty() && !line.startsWith("[")) {
                // Move line
                if (currentGame != null) {
                    currentGame.moves = line;
                    games.add(currentGame);
                    currentGame = null;
                }
            }
        }

        return games;
    }

    /**
     * Parse PGN header line
     */
    private void parseHeader(String line, PGNGameData game) {
        if (line.startsWith("[White \"")) {
            game.whitePlayer = extractHeaderValue(line);
        } else if (line.startsWith("[Black \"")) {
            game.blackPlayer = extractHeaderValue(line);
        } else if (line.startsWith("[Date \"")) {
            game.date = extractHeaderValue(line);
        }
    }

    /**
     * Extract value from PGN header
     */
    private String extractHeaderValue(String header) {
        int start = header.indexOf('"') + 1;
        int end = header.lastIndexOf('"');
        return header.substring(start, end);
    }

    /**
     * Save parsed PGN game to database
     */
    private boolean saveGameToDatabase(PGNGameData gameData) {
        try {
            // Find or create players
            int whitePlayerId = findOrCreatePlayer(gameData.whitePlayer);
            int blackPlayerId = findOrCreatePlayer(gameData.blackPlayer);

            if (whitePlayerId == -1 || blackPlayerId == -1) {
                logger.warn("Could not find or create players for game: {} vs {}",
                        gameData.whitePlayer, gameData.blackPlayer);
                return false;
            }

            // Convert PGN moves back to internal format
            String gameplay = convertPGNToGameplay(gameData.moves);

            // Create game
            Game game = new Game(
                    0, // ID will be generated
                    whitePlayerId,
                    blackPlayerId,
                    Instant.now(), // Use current time since PGN date parsing can be complex
                    gameplay
            );

            Optional<Game> savedGame = gameRepository.save(game);
            return savedGame.isPresent();

        } catch (Exception e) {
            logger.error("Error saving PGN game to database", e);
            return false;
        }
    }

    /**
     * Find existing player or create new one
     */
    private int findOrCreatePlayer(String username) {
        if (username == null || username.trim().isEmpty() || "Unknown".equals(username)) {
            return -1;
        }

        Optional<Player> existingPlayer = playerRepository.findByUsername(username);
        if (existingPlayer.isPresent()) {
            return existingPlayer.get().id();
        }

        // Create new player with default email and password
        String email = username.toLowerCase() + "@imported.chess";
        String password = "imported123"; // Default password for imported players

        boolean created = playerRepository.save(username, email, password);
        if (created) {
            Optional<Player> newPlayer = playerRepository.findByUsername(username);
            return newPlayer.map(Player::id).orElse(-1);
        }

        return -1;
    }

    /**
     * Convert PGN moves back to internal gameplay format
     */
    private String convertPGNToGameplay(String pgnMoves) {
        // This is a simplified conversion - in a full implementation,
        // you'd need to parse algebraic notation and convert to coordinates
        // For now, we'll store the PGN as-is and handle conversion later
        return "PGN:" + pgnMoves;
    }

    /**
     * Data class to hold parsed PGN game information
     */
    private static class PGNGameData {
        String whitePlayer;
        String blackPlayer;
        String date;
        String moves;
    }
}