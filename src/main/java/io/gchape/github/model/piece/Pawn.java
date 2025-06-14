package io.gchape.github.model.piece;



import java.util.ArrayList;
import java.util.List;
import io.gchape.github.model.*;



public class Pawn extends Piece {
    private boolean hasMoved;

    public Pawn(PieceColor color, Position position) {
        super(color, position);
        this.hasMoved = false;
    }

    @Override
    protected String determineImageFile() {
        return getColor() == PieceColor.WHITE ? "wp.png" : "bp.png";
    }

    @Override
    public List<Move> getLegalMoves(Board board) {
        List<Move> legalMoves = new ArrayList<>();
        Position position = getPosition();
        int x = position.getX();
        int y = position.getY();
        int direction = getColor() == PieceColor.WHITE ? -1 : 1; // Moving up or down

        // One square forward
        Position oneForward = new Position(x, y + direction);
        if (board.isValidPosition(oneForward) && board.getPiece(oneForward) == null) {
            // Check if pawn reaches the end of the board (promotion)
            if ((getColor() == PieceColor.WHITE && oneForward.getY() == 0) ||
                    (getColor() == PieceColor.BLACK && oneForward.getY() == 7)) {
                addPromotionMoves(board, position, oneForward, legalMoves);
            } else {
                addMoveIfNotInCheck(board, position, oneForward, legalMoves);
            }

            // Two squares forward (only if the pawn hasn't moved yet)
            if (!hasMoved) {
                Position twoForward = new Position(x, y + 2 * direction);
                if (board.isValidPosition(twoForward) && board.getPiece(twoForward) == null) {
                    Move move = new Move.Builder()
                            .from(position)
                            .to(twoForward)
                            .piece(this)
                            .isPawnTwoSquareMove(true)
                            .build();

                    if (!wouldLeaveKingInCheck(board, move)) {
                        legalMoves.add(move);
                    }
                }
            }
        }

        // Diagonal captures
        addDiagonalCaptures(board, position, x, y, direction, legalMoves);

        // En passant captures
        addEnPassantMoves(board, position, x, y, direction, legalMoves);

        return legalMoves;
    }

    private void addDiagonalCaptures(Board board, Position position, int x, int y, int direction, List<Move> legalMoves) {
        // Left diagonal capture
        Position leftCapture = new Position(x - 1, y + direction);
        if (board.isValidPosition(leftCapture)) {
            Piece targetPiece = board.getPiece(leftCapture);
            if (targetPiece != null && targetPiece.getColor() != getColor()) {
                // Check for promotion
                if ((getColor() == PieceColor.WHITE && leftCapture.getY() == 0) ||
                        (getColor() == PieceColor.BLACK && leftCapture.getY() == 7)) {
                    addPromotionMoves(board, position, leftCapture, legalMoves);
                } else {
                    addMoveIfNotInCheck(board, position, leftCapture, legalMoves);
                }
            }
        }

        // Right diagonal capture
        Position rightCapture = new Position(x + 1, y + direction);
        if (board.isValidPosition(rightCapture)) {
            Piece targetPiece = board.getPiece(rightCapture);
            if (targetPiece != null && targetPiece.getColor() != getColor()) {
                // Check for promotion
                if ((getColor() == PieceColor.WHITE && rightCapture.getY() == 0) ||
                        (getColor() == PieceColor.BLACK && rightCapture.getY() == 7)) {
                    addPromotionMoves(board, position, rightCapture, legalMoves);
                } else {
                    addMoveIfNotInCheck(board, position, rightCapture, legalMoves);
                }
            }
        }
    }

    private void addEnPassantMoves(Board board, Position position, int x, int y, int direction, List<Move> legalMoves) {
        // En passant capture
        Move lastMove = board.getLastMove();
        if (lastMove != null && lastMove.getPiece() instanceof Pawn) {
            Pawn lastMovedPawn = (Pawn) lastMove.getPiece();

            // Check if the last move was a two-square pawn move
            if (lastMove.isPawnTwoSquareMove() &&
                    Math.abs(lastMove.getFrom().getY() - lastMove.getTo().getY()) == 2) {

                Position lastPawnPos = lastMove.getTo();

                // Check if the last moved pawn is adjacent to our pawn
                if (lastPawnPos.getY() == y &&
                        (lastPawnPos.getX() == x - 1 || lastPawnPos.getX() == x + 1)) {

                    // Create en passant move
                    Position enPassantPos = new Position(lastPawnPos.getX(), y + direction);

                    Move enPassantMove = new Move.Builder()
                            .from(position)
                            .to(enPassantPos)
                            .piece(this)
                            .capturedPiece(lastMovedPawn)
                            .isEnPassant(true)
                            .capturedPiecePosition(lastPawnPos)
                            .build();

                    if (!wouldLeaveKingInCheck(board, enPassantMove)) {
                        legalMoves.add(enPassantMove);
                    }
                }
            }
        }
    }

    private void addPromotionMoves(Board board, Position from, Position to, List<Move> moves) {
        Piece capturedPiece = board.getPiece(to);

        // Create promotion moves for Queen, Rook, Bishop, and Knight
        addPromotionMove(board, from, to, capturedPiece, "Queen", moves);
        addPromotionMove(board, from, to, capturedPiece, "Rook", moves);
        addPromotionMove(board, from, to, capturedPiece, "Bishop", moves);
        addPromotionMove(board, from, to, capturedPiece, "Knight", moves);
    }

    private void addPromotionMove(Board board, Position from, Position to, Piece capturedPiece,
                                  String promotionType, List<Move> moves) {
        Move promotionMove = new Move.Builder()
                .from(from)
                .to(to)
                .piece(this)
                .capturedPiece(capturedPiece)
                .isPromotion(true)
                .promotionType(promotionType)
                .build();

        if (!wouldLeaveKingInCheck(board, promotionMove)) {
            moves.add(promotionMove);
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
        int direction = getColor() == PieceColor.WHITE ? -1 : 1;

        // Pawns attack diagonally
        Position leftAttack = new Position(x - 1, y + direction);
        if (board.isValidPosition(leftAttack)) {
            attackPositions.add(leftAttack);
        }

        Position rightAttack = new Position(x + 1, y + direction);
        if (board.isValidPosition(rightAttack)) {
            attackPositions.add(rightAttack);
        }

        return attackPositions;
    }

    @Override
    public boolean moveTo(Position position) {
        boolean moved = super.moveTo(position);
        if (moved) {
            hasMoved = true;
        }
        return moved;
    }

    @Override
    public Piece copy() {
        Pawn copiedPawn = new Pawn(getColor(), new Position(getPosition().getX(), getPosition().getY()));
        copiedPawn.hasMoved = this.hasMoved;
        return copiedPawn;
    }

    @Override
    public String getType() {
        return "Pawn";
    }

    // Getter and setter for hasMoved
    public boolean getHasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }
}