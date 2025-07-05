package io.gchape.github.model.service;

import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for processing gameplay strings and moves
 */
@Component
public class GameplayUtils {
    private static final Logger logger = LoggerFactory.getLogger(GameplayUtils.class);

    // Pattern to match move tuples like (2,3)->(2,4)
    private static final Pattern MOVE_PATTERN = Pattern.compile("\\((\\d+),(\\d+)\\)->\\((\\d+),(\\d+)\\)");

    /**
     * Parse moves from gameplay string
     */
    public List<Move> parseMovesFromGameplay(String gameplay) {
        List<Move> moves = new ArrayList<>();

        if (gameplay == null || gameplay.trim().isEmpty()) {
            return moves;
        }

        try {
            Matcher matcher = MOVE_PATTERN.matcher(gameplay);

            while (matcher.find()) {
                int fromRow = Integer.parseInt(matcher.group(1));
                int fromCol = Integer.parseInt(matcher.group(2));
                int toRow = Integer.parseInt(matcher.group(3));
                int toCol = Integer.parseInt(matcher.group(4));

                Position from = new Position(fromRow, fromCol);
                Position to = new Position(toRow, toCol);

                // Validate positions are within bounds
                if (from.isInBounds() && to.isInBounds()) {
                    moves.add(new Move(from, to));
                } else {
                    logger.warn("Invalid move positions: {} -> {}", from, to);
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing moves from gameplay: {}", gameplay, e);
        }

        return moves;
    }

    /**
     * Convert list of moves to gameplay string
     */
    public String movesToGameplayString(List<Move> moves) {
        if (moves == null || moves.isEmpty()) {
            return "";
        }

        StringBuilder gameplay = new StringBuilder();

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            gameplay.append(moveToString(move));

            if (i < moves.size() - 1) {
                gameplay.append(" ");
            }
        }

        return gameplay.toString();
    }

    /**
     * Convert single move to string format
     */
    public String moveToString(Move move) {
        return String.format("(%d,%d)->(%d,%d)",
                move.from().row(), move.from().col(),
                move.to().row(), move.to().col());
    }

    /**
     * Validate gameplay string format
     */
    public boolean isValidGameplayFormat(String gameplay) {
        if (gameplay == null || gameplay.trim().isEmpty()) {
            return true; // Empty gameplay is valid (no moves yet)
        }

        try {
            // Split by spaces and validate each move
            String[] moveStrings = gameplay.trim().split("\\s+");

            for (String moveString : moveStrings) {
                if (!MOVE_PATTERN.matcher(moveString).matches()) {
                    logger.debug("Invalid move format: {}", moveString);
                    return false;
                }

                // Parse and validate positions
                Matcher matcher = MOVE_PATTERN.matcher(moveString);
                if (matcher.find()) {
                    int fromRow = Integer.parseInt(matcher.group(1));
                    int fromCol = Integer.parseInt(matcher.group(2));
                    int toRow = Integer.parseInt(matcher.group(3));
                    int toCol = Integer.parseInt(matcher.group(4));

                    Position from = new Position(fromRow, fromCol);
                    Position to = new Position(toRow, toCol);

                    if (!from.isInBounds() || !to.isInBounds()) {
                        logger.debug("Move positions out of bounds: {} -> {}", from, to);
                        return false;
                    }
                }
            }

            return true;

        } catch (Exception e) {
            logger.debug("Error validating gameplay format: {}", gameplay, e);
            return false;
        }
    }

    /**
     * Add a move to existing gameplay string
     */
    public String addMoveToGameplay(String existingGameplay, Move move) {
        if (existingGameplay == null || existingGameplay.trim().isEmpty()) {
            return moveToString(move);
        }

        return existingGameplay + " " + moveToString(move);
    }

    /**
     * Get the last move from gameplay string
     */
    public Move getLastMove(String gameplay) {
        List<Move> moves = parseMovesFromGameplay(gameplay);
        return moves.isEmpty() ? null : moves.get(moves.size() - 1);
    }

    /**
     * Get total number of moves in gameplay
     */
    public int getMoveCount(String gameplay) {
        return parseMovesFromGameplay(gameplay).size();
    }

    /**
     * Check if it's white's turn based on move count
     */
    public boolean isWhiteTurn(String gameplay) {
        int moveCount = getMoveCount(gameplay);
        return moveCount % 2 == 0; // Even number of moves = white's turn
    }

    /**
     * Get moves for a specific player color
     */
    public List<Move> getMovesForColor(String gameplay, boolean isWhite) {
        List<Move> allMoves = parseMovesFromGameplay(gameplay);
        List<Move> colorMoves = new ArrayList<>();

        for (int i = 0; i < allMoves.size(); i++) {
            // White moves are at even indices (0, 2, 4, ...)
            // Black moves are at odd indices (1, 3, 5, ...)
            if ((i % 2 == 0) == isWhite) {
                colorMoves.add(allMoves.get(i));
            }
        }

        return colorMoves;
    }

    /**
     * Create sample gameplay string for testing
     */
    public String createSampleGameplay() {
        List<Move> sampleMoves = List.of(
                new Move(new Position(6, 4), new Position(4, 4)), // e2-e4
                new Move(new Position(1, 4), new Position(3, 4)), // e7-e5
                new Move(new Position(7, 6), new Position(5, 5)), // g1-f3
                new Move(new Position(0, 1), new Position(2, 2)), // b8-c6
                new Move(new Position(7, 5), new Position(4, 2)), // f1-c4
                new Move(new Position(0, 5), new Position(3, 2))  // f8-c5
        );

        return movesToGameplayString(sampleMoves);
    }

    /**
     * Validate a single move format
     */
    public boolean isValidMoveFormat(String moveString) {
        return MOVE_PATTERN.matcher(moveString).matches();
    }

    /**
     * Clean up gameplay string (remove extra spaces, etc.)
     */
    public String cleanupGameplay(String gameplay) {
        if (gameplay == null) {
            return "";
        }

        return gameplay.trim().replaceAll("\\s+", " ");
    }
}