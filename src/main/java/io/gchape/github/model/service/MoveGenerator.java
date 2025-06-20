package io.gchape.github.model.service;

import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MoveGenerator {
    private static final int BOARD_SIZE = 8;

    public List<Position> generateValidMoves(final GameState gameState, final Position from) {
        Piece piece = gameState.getPieceAt(from);
        if (piece == null) return new ArrayList<>();

        List<Position> moves = new ArrayList<>();
        boolean isWhite = piece.color == 'w';

        switch (piece) {
            case WP, BP -> generatePawnMoves(moves, gameState, from, isWhite);
            case WR, BR -> generateRookMoves(moves, gameState, from, piece.color);
            case WN, BN -> generateKnightMoves(moves, gameState, from, piece.color);
            case WB, BB -> generateBishopMoves(moves, gameState, from, piece.color);
            case WQ, BQ -> generateQueenMoves(moves, gameState, from, piece.color);
            case WK, BK -> generateKingMoves(moves, gameState, from, piece.color);
        }
        return moves;
    }

    private void generatePawnMoves(final List<Position> moves,
                                   final GameState gameState,
                                   final Position from,
                                   final boolean isWhite) {
        int direction = isWhite ? -1 : 1;
        int startRow = isWhite ? 6 : 1;

        Position forward = new Position(from.row() + direction, from.col());
        if (forward.isInBounds() && gameState.getPieceAt(forward) == null) {
            moves.add(forward);

            if (from.row() == startRow) {
                Position doubleForward = new Position(from.row() + 2 * direction, from.col());
                if (doubleForward.isInBounds() && gameState.getPieceAt(doubleForward) == null) {
                    moves.add(doubleForward);
                }
            }
        }

        for (int dc : new int[]{-1, 1}) {
            Position capture = new Position(from.row() + direction, from.col() + dc);
            if (capture.isInBounds()) {
                Piece target = gameState.getPieceAt(capture);
                if (target != null && target.color != (isWhite ? 'w' : 'b')) {
                    moves.add(capture);
                }
            }
        }
    }

    private void generateRookMoves(final List<Position> moves,
                                   final GameState gameState,
                                   final Position from,
                                   final char color) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        generateSlidingMoves(moves, gameState, from, color, directions);
    }

    private void generateBishopMoves(final List<Position> moves,
                                     final GameState gameState,
                                     final Position from,
                                     final char color) {
        int[][] directions = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        generateSlidingMoves(moves, gameState, from, color, directions);
    }

    private void generateQueenMoves(final List<Position> moves,
                                    final GameState gameState,
                                    final Position from,
                                    final char color) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        generateSlidingMoves(moves, gameState, from, color, directions);
    }

    private void generateSlidingMoves(final List<Position> moves,
                                      final GameState gameState,
                                      final Position from,
                                      final char color,
                                      final int[][] directions) {
        for (int[] dir : directions) {
            for (int i = 1; i < BOARD_SIZE; i++) {
                Position newPos = new Position(from.row() + dir[0] * i, from.col() + dir[1] * i);

                if (!newPos.isInBounds()) break;

                Piece target = gameState.getPieceAt(newPos);
                if (target == null) {
                    moves.add(newPos);
                } else {
                    if (target.color != color) {
                        moves.add(newPos);
                    }
                    break;
                }
            }
        }
    }

    private void generateKnightMoves(final List<Position> moves,
                                     final GameState gameState,
                                     final Position from,
                                     final char color) {
        int[][] knightMoves = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        generateFixedMoves(moves, gameState, from, color, knightMoves);
    }

    private void generateKingMoves(final List<Position> moves,
                                   final GameState gameState,
                                   final Position from,
                                   final char color) {
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        generateFixedMoves(moves, gameState, from, color, directions);
    }

    private void generateFixedMoves(final List<Position> moves,
                                    final GameState gameState,
                                    final Position from,
                                    final char color,
                                    final int[][] moveOffsets) {
        for (int[] offset : moveOffsets) {
            Position newPos = new Position(from.row() + offset[0], from.col() + offset[1]);

            if (newPos.isInBounds()) {
                Piece target = gameState.getPieceAt(newPos);
                if (target == null || target.color != color) {
                    moves.add(newPos);
                }
            }
        }
    }
}
