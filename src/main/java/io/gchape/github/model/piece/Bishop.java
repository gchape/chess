package io.gchape.github.model.piece;

import io.gchape.github.model.*;

import java.util.ArrayList;
import java.util.List;



public class Bishop extends Piece {

    public Bishop(PieceColor color, Position position) {
        super(color, position);
    }

    @Override
    protected String determineImageFile() {
        return getColor() == PieceColor.WHITE ? "wb.png" : "bb.png";
    }

    @Override
    public List<Move> getLegalMoves(Board board) {
        List<Move> legalMoves = new ArrayList<>();
        Position position = getPosition();
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

        return legalMoves;
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
        return "Bishop";
    }

    @Override
    public Piece copy() {
        return new Bishop(getColor(), getPosition());
    }
}