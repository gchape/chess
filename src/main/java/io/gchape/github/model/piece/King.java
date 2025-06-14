package io.gchape.github.model.piece;


import io.gchape.github.model.*;

import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    // Add a field to track if the king has moved (needed for castling)
    private boolean hasMoved;

    public King(PieceColor color, Position position) {
        super(color, position);
        this.hasMoved = false;
    }

    @Override
    protected String determineImageFile() {
        return getColor() == PieceColor.WHITE ? "wk.png" : "bk.png";
    }

    @Override
    public List<Move> getLegalMoves(Board board) {
        List<Move> legalMoves = new ArrayList<>();
        Position position = getPosition();
        int x = position.getX();
        int y = position.getY();

        // Check all 8 squares around the king
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                // Skip the current position
                if (dx == 0 && dy == 0) {
                    continue;
                }

                Position targetPos = new Position(x + dx, y + dy);

                if (board.isValidPosition(targetPos)) {
                    Piece targetPiece = board.getPiece(targetPos);

                    // Empty square or opponent's piece
                    if (targetPiece == null || targetPiece.getColor() != getColor()) {
                        // For the king, we need to check if the new position is under attack
                        if (!isPositionUnderAttack(board, targetPos)) {
                            Move move = new Move.Builder()
                                    .from(position)
                                    .to(targetPos)
                                    .piece(this)
                                    .capturedPiece(targetPiece)
                                    .build();

                            if (!wouldLeaveKingInCheck(board, move)) {
                                legalMoves.add(move);
                            }
                        }
                    }
                }
            }
        }

        // Add castling moves if conditions are met
        addCastlingMoves(board, legalMoves);

        return legalMoves;
    }

    /**
     * Add castling moves if the conditions are met:
     * 1. King has not moved
     * 2. Rook has not moved
     * 3. No pieces between king and rook
     * 4. King is not in check
     * 5. King does not pass through a square that is attacked
     */
    private void addCastlingMoves(Board board, List<Move> legalMoves) {
        if (hasMoved || board.isInCheck(getColor())) {
            return; // Can't castle if king has moved or is in check
        }

        Position position = getPosition();
        int y = position.getY();

        // Kingside castling (O-O)
        addKingsideCastlingMove(board, legalMoves, y);

        // Queenside castling (O-O-O)
        addQueensideCastlingMove(board, legalMoves, y);
    }

    private void addKingsideCastlingMove(Board board, List<Move> legalMoves, int y) {
        // Check kingside castling
        Position rookPosition = new Position(7, y);
        Piece rook = board.getPiece(rookPosition);

        if (rook instanceof Rook && rook.getColor() == getColor() && !((Rook) rook).getHasMoved()) {
            // Check if squares between king and rook are empty
            boolean pathClear = true;
            for (int x = 5; x < 7; x++) {
                Position pos = new Position(x, y);
                if (board.getPiece(pos) != null) {
                    pathClear = false;
                    break;
                }
                // Also check if the square the king passes through is under attack
                if (x == 5 && isPositionUnderAttack(board, pos)) {
                    pathClear = false;
                    break;
                }
            }

            if (pathClear) {
                // Create a castling move
                Position kingFinalPos = new Position(6, y);
                Position rookFinalPos = new Position(5, y);

                Move castlingMove = new Move.Builder()
                        .from(getPosition())
                        .to(kingFinalPos)
                        .piece(this)
                        .isCastling(true)
                        .castlingRook((Rook) rook)
                        .rookFromPosition(rookPosition)
                        .rookToPosition(rookFinalPos)
                        .build();

                legalMoves.add(castlingMove);
            }
        }
    }

    private void addQueensideCastlingMove(Board board, List<Move> legalMoves, int y) {
        // Check queenside castling
        Position rookPosition = new Position(0, y);
        Piece rook = board.getPiece(rookPosition);

        if (rook instanceof Rook && rook.getColor() == getColor() && !((Rook) rook).getHasMoved()) {
            // Check if squares between king and rook are empty
            boolean pathClear = true;
            for (int x = 1; x < 4; x++) {
                Position pos = new Position(x, y);
                if (board.getPiece(pos) != null) {
                    pathClear = false;
                    break;
                }
                // Also check if the square the king passes through is under attack
                if (x == 3 && isPositionUnderAttack(board, pos)) {
                    pathClear = false;
                    break;
                }
            }

            if (pathClear) {
                // Create a castling move
                Position kingFinalPos = new Position(2, y);
                Position rookFinalPos = new Position(3, y);

                Move castlingMove = new Move.Builder()
                        .from(getPosition())
                        .to(kingFinalPos)
                        .piece(this)
                        .isCastling(true)
                        .castlingRook((Rook) rook)
                        .rookFromPosition(rookPosition)
                        .rookToPosition(rookFinalPos)
                        .build();

                legalMoves.add(castlingMove);
            }
        }
    }

    /**
     * Check if a position is under attack by any of the opponent's pieces
     */
    private boolean isPositionUnderAttack(Board board, Position position) {
        PieceColor opponentColor = getColor() == PieceColor.WHITE ?
                PieceColor.BLACK : PieceColor.WHITE;

        List<Piece> opponentPieces = board.getPieces(opponentColor);

        for (Piece piece : opponentPieces) {
            List<Position> attackPositions = piece.getAttackPositions(board);

            for (Position attackPos : attackPositions) {
                if (attackPos.equals(position)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<Position> getAttackPositions(Board board) {
        List<Position> attackPositions = new ArrayList<>();
        Position position = getPosition();
        int x = position.getX();
        int y = position.getY();

        // Check all 8 squares around the king
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                // Skip the current position
                if (dx == 0 && dy == 0) {
                    continue;
                }

                Position targetPos = new Position(x + dx, y + dy);

                if (board.isValidPosition(targetPos)) {
                    attackPositions.add(targetPos);
                }
            }
        }

        return attackPositions;
    }

    @Override
    public String getType() {
        return "King";
    }

    @Override
    public Piece copy() {
        King copiedKing = new King(getColor(), new Position(getPosition().getX(), getPosition().getY()));
        copiedKing.hasMoved = this.hasMoved;
        return copiedKing;
    }

    @Override
    public boolean moveTo(Position position) {
        boolean moved = super.moveTo(position);
        if (moved) {
            hasMoved = true;
        }
        return moved;
    }

    // Getter and setter for hasMoved (needed for tests)
    public boolean getHasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }
}