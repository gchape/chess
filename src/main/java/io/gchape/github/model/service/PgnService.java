package io.gchape.github.model.service;

import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import io.gchape.github.model.entity.db.Game;
import io.gchape.github.model.entity.db.Player;
import io.gchape.github.model.repository.GameRepository;
import io.gchape.github.model.repository.PlayerRepository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PgnService {
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;

    // PGN patterns for parsing
    private static final Pattern TAG_PATTERN = Pattern.compile("\\[([^\\]]+)\\s+\"([^\"]+)\"\\]");
    private static final Pattern MOVE_PATTERN = Pattern.compile("(\\d+)\\.\\s*([^\\s]+)(?:\\s+([^\\s]+))?");
    private static final Pattern RESULT_PATTERN = Pattern.compile("(1-0|0-1|1/2-1/2|\\*)");

    public PgnService(GameRepository gameRepository, PlayerRepository playerRepository) {
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
    }

    /**
     * Export a single game to PGN format
     */
    public String exportGameToPgn(int gameId) {
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isEmpty()) {
            throw new IllegalArgumentException("Game not found: " + gameId);
        }

        Game game = gameOpt.get();
        return convertGameToPgn(game);
    }

    /**
     * Export multiple games to PGN format
     */
    public String exportGamesToPgn(List<Integer> gameIds) {
        StringBuilder pgnContent = new StringBuilder();

        for (int gameId : gameIds) {
            String gamePgn = exportGameToPgn(gameId);
            pgnContent.append(gamePgn).append("\n\n");
        }

        return pgnContent.toString();
    }

    /**
     * Export all games by a player to PGN format
     */
    public String exportPlayerGamesToPgn(int playerId) {
        List<Game> games = gameRepository.findByPlayerId(playerId);
        StringBuilder pgnContent = new StringBuilder();

        for (Game game : games) {
            String gamePgn = convertGameToPgn(game);
            pgnContent.append(gamePgn).append("\n\n");
        }

        return pgnContent.toString();
    }

    /**
     * Save PGN content to file
     */
    public void savePgnToFile(String pgnContent, String filePath) throws IOException {
        Files.writeString(Path.of(filePath), pgnContent);
    }

    /**
     * Load and parse PGN file, returning list of parsed games
     */
    public List<ParsedPgnGame> loadPgnFromFile(String filePath) throws IOException {
        String content = Files.readString(Path.of(filePath));
        return parsePgnContent(content);
    }

    /**
     * Import games from PGN file into database
     */
    public List<Game> importPgnToDatabase(String filePath, int defaultWhitePlayerId, int defaultBlackPlayerId) throws IOException {
        List<ParsedPgnGame> parsedGames = loadPgnFromFile(filePath);
        List<Game> savedGames = new ArrayList<>();

        for (ParsedPgnGame parsedGame : parsedGames) {
            Game game = convertParsedGameToEntity(parsedGame, defaultWhitePlayerId, defaultBlackPlayerId);
            Optional<Game> savedGame = gameRepository.save(game);
            savedGame.ifPresent(savedGames::add);
        }

        return savedGames;
    }

    /**
     * Convert internal Game entity to PGN format
     */
    private String convertGameToPgn(Game game) {
        StringBuilder pgn = new StringBuilder();

        // Get player names
        String whiteName = getPlayerName(game.playerWhiteId());
        String blackName = getPlayerName(game.playerBlackId());

        // PGN Headers (Seven Tag Roster)
        pgn.append("[Event \"Chess Game\"]\n");
        pgn.append("[Site \"Chess Application\"]\n");
        pgn.append("[Date \"").append(formatDate(game.startTime())).append("\"]\n");
        pgn.append("[Round \"1\"]\n");
        pgn.append("[White \"").append(whiteName).append("\"]\n");
        pgn.append("[Black \"").append(blackName).append("\"]\n");
        pgn.append("[Result \"*\"]\n"); // Default result, could be enhanced
        pgn.append("\n");

        // Convert gameplay to PGN moves
        String moves = convertGameplayToPgnMoves(game.gameplay());
        pgn.append(moves);

        return pgn.toString();
    }

    /**
     * Convert your internal gameplay format to PGN algebraic notation
     */
    private String convertGameplayToPgnMoves(String gameplay) {
        if (gameplay == null || gameplay.trim().isEmpty()) {
            return "*";
        }

        StringBuilder pgnMoves = new StringBuilder();
        String[] moves = gameplay.split("\\|");

        for (int i = 0; i < moves.length; i++) {
            String move = moves[i].trim();
            if (move.isEmpty()) continue;

            // Parse move from your format: "from->to"
            Move parsedMove = parseMoveFromGameplay(move);
            if (parsedMove != null) {
                String algebraicMove = convertToAlgebraicNotation(parsedMove, i % 2 == 0);

                if (i % 2 == 0) {
                    // White move
                    pgnMoves.append((i / 2) + 1).append(". ").append(algebraicMove);
                } else {
                    // Black move
                    pgnMoves.append(" ").append(algebraicMove);
                    if (i < moves.length - 1) pgnMoves.append(" ");
                }
            }
        }

        pgnMoves.append(" *"); // Game result placeholder
        return pgnMoves.toString();
    }

    /**
     * Convert Position to algebraic notation (e.g., Position(0,0) -> "a8")
     */
    private String positionToAlgebraic(Position pos) {
        char file = (char) ('a' + pos.col());
        int rank = 8 - pos.row();
        return "" + file + rank;
    }

    /**
     * Convert algebraic notation to Position (e.g., "a8" -> Position(0,0))
     */
    private Position algebraicToPosition(String algebraic) {
        if (algebraic.length() != 2) return null;

        char file = algebraic.charAt(0);
        char rank = algebraic.charAt(1);

        int col = file - 'a';
        int row = 8 - (rank - '0');

        return new Position(row, col);
    }

    /**
     * Convert move to algebraic notation (simplified version)
     */
    private String convertToAlgebraicNotation(Move move, boolean isWhite) {
        String from = positionToAlgebraic(move.from());
        String to = positionToAlgebraic(move.to());

        // This is a simplified conversion - for a complete implementation,
        // you'd need to determine piece types, check for captures, etc.
        return from + to;
    }

    /**
     * Parse move from your gameplay format
     */
    private Move parseMoveFromGameplay(String moveStr) {
        try {
            // Assuming format like "(0,1)->(0,3)"
            String[] parts = moveStr.split("->");
            if (parts.length != 2) return null;

            Position from = parsePositionFromString(parts[0].trim());
            Position to = parsePositionFromString(parts[1].trim());

            return new Move(from, to);
        } catch (Exception e) {
            System.err.println("Failed to parse move: " + moveStr);
            return null;
        }
    }

    /**
     * Parse position from string format "(row,col)"
     */
    private Position parsePositionFromString(String posStr) {
        String cleaned = posStr.replaceAll("[\\(\\)]", "");
        String[] coords = cleaned.split(",");
        int row = Integer.parseInt(coords[0].trim());
        int col = Integer.parseInt(coords[1].trim());
        return new Position(row, col);
    }

    /**
     * Parse PGN content into structured data
     */
    private List<ParsedPgnGame> parsePgnContent(String content) {
        List<ParsedPgnGame> games = new ArrayList<>();
        String[] gameStrings = content.split("(?=\\[Event)");

        for (String gameString : gameStrings) {
            if (gameString.trim().isEmpty()) continue;

            ParsedPgnGame parsedGame = parseSinglePgnGame(gameString.trim());
            if (parsedGame != null) {
                games.add(parsedGame);
            }
        }

        return games;
    }

    /**
     * Parse a single PGN game
     */
    private ParsedPgnGame parseSinglePgnGame(String gameString) {
        Map<String, String> headers = new HashMap<>();
        List<String> moves = new ArrayList<>();

        String[] lines = gameString.split("\n");
        boolean inMoveSection = false;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                // Parse header
                Matcher matcher = TAG_PATTERN.matcher(line);
                if (matcher.find()) {
                    headers.put(matcher.group(1), matcher.group(2));
                }
            } else if (!inMoveSection && !line.startsWith("[")) {
                inMoveSection = true;
            }

            if (inMoveSection) {
                // Parse moves
                parseMoveText(line, moves);
            }
        }

        return new ParsedPgnGame(headers, moves);
    }

    /**
     * Parse move text and extract individual moves
     */
    private void parseMoveText(String moveText, List<String> moves) {
        // Remove comments and variations
        moveText = moveText.replaceAll("\\{[^}]*\\}", "");
        moveText = moveText.replaceAll("\\([^)]*\\)", "");

        Matcher matcher = MOVE_PATTERN.matcher(moveText);
        while (matcher.find()) {
            String whiteMove = matcher.group(2);
            String blackMove = matcher.group(3);

            if (whiteMove != null && !isGameResult(whiteMove)) {
                moves.add(whiteMove);
            }
            if (blackMove != null && !isGameResult(blackMove)) {
                moves.add(blackMove);
            }
        }
    }

    /**
     * Check if string is a game result
     */
    private boolean isGameResult(String text) {
        return RESULT_PATTERN.matcher(text).matches();
    }

    /**
     * Convert parsed PGN game to database entity
     */
    private Game convertParsedGameToEntity(ParsedPgnGame parsedGame, int defaultWhiteId, int defaultBlackId) {
        // Try to find players by name, fallback to default IDs
        int whiteId = findPlayerByName(parsedGame.headers.get("White")).orElse(defaultWhiteId);
        int blackId = findPlayerByName(parsedGame.headers.get("Black")).orElse(defaultBlackId);

        // Convert PGN moves back to your internal format
        String gameplay = convertPgnMovesToGameplay(parsedGame.moves);

        // Parse date or use current time
        var startTime = parsePgnDate(parsedGame.headers.get("Date"))
                .orElse(LocalDateTime.now().toInstant(ZoneOffset.UTC));

        return new Game(0, whiteId, blackId, startTime, gameplay);
    }

    /**
     * Convert PGN moves to your internal gameplay format
     */
    private String convertPgnMovesToGameplay(List<String> pgnMoves) {
        StringBuilder gameplay = new StringBuilder();

        for (int i = 0; i < pgnMoves.size(); i++) {
            String pgnMove = pgnMoves.get(i);
            String internalMove = convertPgnMoveToInternal(pgnMove);

            if (internalMove != null) {
                if (i > 0) gameplay.append("|");
                gameplay.append(internalMove);
            }
        }

        return gameplay.toString();
    }

    /**
     * Convert single PGN move to internal format (simplified)
     */
    private String convertPgnMoveToInternal(String pgnMove) {
        // This is a very simplified conversion
        // For a complete implementation, you'd need a full chess engine
        // to properly convert algebraic notation back to coordinates

        if (pgnMove.length() >= 4) {
            try {
                String fromAlg = pgnMove.substring(0, 2);
                String toAlg = pgnMove.substring(2, 4);

                Position from = algebraicToPosition(fromAlg);
                Position to = algebraicToPosition(toAlg);

                if (from != null && to != null) {
                    return from.toString() + "->" + to.toString();
                }
            } catch (Exception e) {
                System.err.println("Failed to convert PGN move: " + pgnMove);
            }
        }

        return null;
    }

    // Helper methods
    private String getPlayerName(int playerId) {
        return playerRepository.findById(playerId)
                .map(Player::username)
                .orElse("Unknown Player");
    }

    private Optional<Integer> findPlayerByName(String playerName) {
        if (playerName == null) return Optional.empty();

        return playerRepository.findByUsername(playerName)
                .map(Player::id);
    }

    private String formatDate(java.time.Instant instant) {
        return instant.atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
    }

    private Optional<java.time.Instant> parsePgnDate(String dateStr) {
        if (dateStr == null || dateStr.equals("????.??.??")) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    LocalDateTime.parse(dateStr + " 00:00:00",
                                    DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                            .toInstant(ZoneOffset.UTC)
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Data class for parsed PGN games
     */
    public static class ParsedPgnGame {
        public final Map<String, String> headers;
        public final List<String> moves;

        public ParsedPgnGame(Map<String, String> headers, List<String> moves) {
            this.headers = headers;
            this.moves = moves;
        }
    }
}