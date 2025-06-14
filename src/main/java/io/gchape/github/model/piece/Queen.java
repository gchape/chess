package io.gchape.github.model.piece;


import java.util.ArrayList;
import java.util.List;
import io.gchape.github.model.*;



public class Queen extends Piece {

    public Queen(PieceColor color, Position position) {
        super(color, position);
    }

    @Override
    protected String determineImageFile() {
        return getColor() == PieceColor.WHITE ? "wq.png" : "bq.png";
    }

    @Override
    public List<Move> getLegalMoves(Board board) {
        List<Move> legalMoves = new ArrayList<>();
        Position position = getPosition();
        int x = position.getX();
        int y = position.getY();

        // The queen moves like a rook and bishop combined
        // First, add all horizontal and vertical moves (like a rook)
        addStraightMoves(board, position, legalMoves);

        // Then add all diagonal moves (like a bishop)
        addDiagonalMoves(board, position, legalMoves);

        return legalMoves;
    }

    private void addStraightMoves(Board board, Position position, List<Move> legalMoves) {
        int x = position.getX();
        int y = position.getY();

        // Horizontal and vertical directions: right, left, down, up
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] direction : directions) {
            int dx = direction[0];
            int dy = direction[1];

            // Move in the direction until hitting a piece or the edge of the board
            for (int i = 1; i < 8; i++) {
                Position targetPos = new Position(x + i * dx, y + i * dy);

                if (!board.isValidPosition(targetPos)) {
                    break; // Hit the edge of the board
                }

                Piece targetPiece = board.getPiece(targetPos);

                if (targetPiece == null) {
                    // Empty square, add as a legal move
                    addMoveIfNotInCheck(board, position, targetPos, legalMoves);
                } else {
                    // Hit a piece
                    if (targetPiece.getColor() != getColor()) {
                        // Can capture opponent's piece
                        addMoveIfNotInCheck(board, position, targetPos, legalMoves);
                    }
                    break; // Can't move past a piece
                }
            }
        }
    }

    private void addDiagonalMoves(Board board, Position position, List<Move> legalMoves) {
        int x = position.getX();
        int y = position.getY();

        // Diagonal directions: up-right, up-left, down-right, down-left
        int[][] directions = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

        for (int[] direction : directions) {
            int dx = direction[0];
            int dy = direction[1];

            // Move in the direction until hitting a piece or the edge of the board
            for (int i = 1; i < 8; i++) {
                Position targetPos = new Position(x + i * dx, y + i * dy);

                if (!board.isValidPosition(targetPos)) {
                    break; // Hit the edge of the board
                }

                Piece targetPiece = board.getPiece(targetPos);

                if (targetPiece == null) {
                    // Empty square, add as a legal move
                    addMoveIfNotInCheck(board, position, targetPos, legalMoves);
                } else {
                    // Hit a piece
                    if (targetPiece.getColor() != getColor()) {
                        // Can capture opponent's piece
                        addMoveIfNotInCheck(board, position, targetPos, legalMoves);
                    }
                    break; // Can't move past a piece
                }
            }
        }
    }

    private void addMoveIfNotInCheck(Board board, Position from, Position to, List<Move> moves) {
        Piece capturedPiece = board.getPiece(to);
        Move move = new Move.Builder()
                .from(from)
                .to(to)
                .piece(this)
                .capturedPiece(capturedPiece)
                .build();

        if (!wouldLeaveKingInCheck(board, move)) {
            moves.add(move);
        }
    }

    @Override
    public List<Position> getAttackPositions(Board board) {
        List<Position> attackPositions = new ArrayList<>();
        Position position = getPosition();
        int x = position.getX();
        int y = position.getY();

        // Horizontal, vertical, and diagonal directions
        int[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},  // Horizontal and vertical
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1}  // Diagonal
        };

        for (int[] direction : directions) {
            int dx = direction[0];
            int dy = direction[1];

            // Move in the direction until hitting a piece or the edge of the board
            for (int i = 1; i < 8; i++) {
                Position targetPos = new Position(x + i * dx, y + i * dy);

                if (!board.isValidPosition(targetPos)) {
                    break; // Hit the edge of the board
                }

                attackPositions.add(targetPos);

                if (board.getPiece(targetPos) != null) {
                    break; // Can't attack past a piece
                }
            }
        }

        return attackPositions;
    }

    @Override
    public String getType() {
        return "Queen";
    }

    @Override
    public Piece copy() {
        return new Queen(getColor(), getPosition());
    }
}