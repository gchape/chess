package io.gchape.github.model.piece;



import java.util.ArrayList;
import java.util.List;

import io.gchape.github.model.*;

public class Knight extends Piece {

    public Knight(PieceColor color, Position position) {
        super(color, position);
    }

    @Override
    protected String determineImageFile() {
        return getColor() == PieceColor.WHITE ? "wn.png" : "bn.png";
    }

    @Override
    public List<Move> getLegalMoves(Board board) {
        List<Move> legalMoves = new ArrayList<>();
        Position position = getPosition();
        int x = position.getX();
        int y = position.getY();

        // Knight move offsets
        int[][] moveOffsets = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        for (int[] offset : moveOffsets) {
            Position targetPos = new Position(x + offset[0], y + offset[1]);

            if (board.isValidPosition(targetPos)) {
                Piece targetPiece = board.getPiece(targetPos);

                // Empty square or opponent's piece
                if (targetPiece == null || targetPiece.getColor() != getColor()) {
                    Move move = new Move.Builder()
                            .from(position)
                            .to(targetPos)
                            .piece(this)
                            .capturedPiece(targetPiece)
                            .build();

                    // Check if the move would leave the king in check
                    if (!wouldLeaveKingInCheck(board, move)) {
                        legalMoves.add(move);
                    }
                }
            }
        }

        return legalMoves;
    }

    @Override
    public List<Position> getAttackPositions(Board board) {
        List<Position> attackPositions = new ArrayList<>();
        Position position = getPosition();
        int x = position.getX();
        int y = position.getY();

        // Knight move offsets
        int[][] moveOffsets = {
                {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
                {1, -2}, {1, 2}, {2, -1}, {2, 1}
        };

        for (int[] offset : moveOffsets) {
            Position targetPos = new Position(x + offset[0], y + offset[1]);

            if (board.isValidPosition(targetPos)) {
                attackPositions.add(targetPos);
            }
        }

        return attackPositions;
    }

    @Override
    public String getType() {
        return "Knight";
    }

    @Override
    public Piece copy() {
        return new Knight(getColor(), getPosition());
    }
}