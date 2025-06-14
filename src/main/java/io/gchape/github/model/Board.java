package io.gchape.github.model;

import io.gchape.github.model.piece.*;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final Piece[][] pieces;
    private List<Piece> whitePieces;
    private List<Piece> blackPieces;
    private List<Move> moveHistory;
    private King whiteKing;
    private King blackKing;

    public Board() {
        pieces = new Piece[8][8];
        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();
        moveHistory = new ArrayList<>();
        initializeBoard();
    }

    /**
     * Copy constructor that creates a deep copy of another board
     * @param original The original board to copy from
     */
    public Board(Board original) {
        // Initialize empty arrays and lists
        this.pieces = new Piece[8][8];
        this.whitePieces = new ArrayList<>();
        this.blackPieces = new ArrayList<>();
        this.moveHistory = new ArrayList<>(original.moveHistory);

        // Copy all pieces
        for (Piece whitePiece : original.whitePieces) {
            Piece copiedPiece = whitePiece.copy();
            whitePieces.add(copiedPiece);
            Position pos = copiedPiece.getPosition();
            if (pos != null) {
                pieces[pos.getY()][pos.getX()] = copiedPiece;
            }

            // Keep track of the king
            if (copiedPiece instanceof King) {
                this.whiteKing = (King) copiedPiece;
            }
        }

        for (Piece blackPiece : original.blackPieces) {
            Piece copiedPiece = blackPiece.copy();
            blackPieces.add(copiedPiece);
            Position pos = copiedPiece.getPosition();
            if (pos != null) {
                pieces[pos.getY()][pos.getX()] = copiedPiece;
            }

            // Keep track of the king
            if (copiedPiece instanceof King) {
                this.blackKing = (King) copiedPiece;
            }
        }
    }

    private void initializeBoard() {
        // Initialize pawns
        for (int x = 0; x < 8; x++) {
            placePiece(new Pawn(PieceColor.BLACK, new Position(x, 1)));
            placePiece(new Pawn(PieceColor.WHITE, new Position(x, 6)));
        }

        // Initialize rooks
        placePiece(new Rook(PieceColor.BLACK, new Position(0, 0)));
        placePiece(new Rook(PieceColor.BLACK, new Position(7, 0)));
        placePiece(new Rook(PieceColor.WHITE, new Position(0, 7)));
        placePiece(new Rook(PieceColor.WHITE, new Position(7, 7)));

        // Initialize knights
        placePiece(new Knight(PieceColor.BLACK, new Position(1, 0)));
        placePiece(new Knight(PieceColor.BLACK, new Position(6, 0)));
        placePiece(new Knight(PieceColor.WHITE, new Position(1, 7)));
        placePiece(new Knight(PieceColor.WHITE, new Position(6, 7)));

        // Initialize bishops
        placePiece(new Bishop(PieceColor.BLACK, new Position(2, 0)));
        placePiece(new Bishop(PieceColor.BLACK, new Position(5, 0)));
        placePiece(new Bishop(PieceColor.WHITE, new Position(2, 7)));
        placePiece(new Bishop(PieceColor.WHITE, new Position(5, 7)));

        // Initialize queens
        placePiece(new Queen(PieceColor.BLACK, new Position(3, 0)));
        placePiece(new Queen(PieceColor.WHITE, new Position(3, 7)));

        // Initialize kings
        blackKing = new King(PieceColor.BLACK, new Position(4, 0));
        whiteKing = new King(PieceColor.WHITE, new Position(4, 7));
        placePiece(blackKing);
        placePiece(whiteKing);
    }

    public Piece getPiece(Position position) {
        if (!isValidPosition(position)) {
            return null;
        }
        return pieces[position.getY()][position.getX()];
    }

    public boolean isValidPosition(Position position) {
        int x = position.getX();
        int y = position.getY();
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public List<Piece> getPieces(PieceColor color) {
        return color == PieceColor.WHITE ? new ArrayList<>(whitePieces) : new ArrayList<>(blackPieces);
    }

    public King getKing(PieceColor color) {
        return color == PieceColor.WHITE ? whiteKing : blackKing;
    }

    /**
     * Places a piece on the board at the piece's position
     * @param piece The piece to place on the board
     * @return true if the piece was placed successfully, false otherwise
     */
    public boolean placePiece(Piece piece) {
        if (piece == null) {
            return false;
        }

        Position pos = piece.getPosition();

        // Check if position is valid
        if (!isValidPosition(pos)) {
            return false;
        }

        // Check if the position is already occupied
        if (getPiece(pos) != null) {
            // Optionally: remove the existing piece first
            removePiece(pos);
        }

        // Place the piece
        pieces[pos.getY()][pos.getX()] = piece;

        // Add to the appropriate list
        if (piece.getColor() == PieceColor.WHITE) {
            whitePieces.add(piece);
            // Track the king if this is a king piece
            if (piece instanceof King) {
                whiteKing = (King) piece;
            }
        } else {
            blackPieces.add(piece);
            // Track the king if this is a king piece
            if (piece instanceof King) {
                blackKing = (King) piece;
            }
        }

        return true;
    }

    public List<Move> getLegalMoves(PieceColor color) {
        List<Move> legalMoves = new ArrayList<>();
        List<Piece> pieces = color == PieceColor.WHITE ? whitePieces : blackPieces;

        for (Piece piece : pieces) {
            legalMoves.addAll(piece.getLegalMoves(this));
        }

        return legalMoves;
    }

    public boolean isInCheck(PieceColor color) {
        King king = color == PieceColor.WHITE ? whiteKing : blackKing;
        Position kingPosition = king.getPosition();
        List<Piece> opponentPieces = color == PieceColor.WHITE ? blackPieces : whitePieces;

        for (Piece piece : opponentPieces) {
            List<Position> attackPositions = piece.getAttackPositions(this);
            if (attackPositions.contains(kingPosition)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCheckmate(PieceColor color) {
        if (!isInCheck(color)) {
            return false;
        }

        List<Move> legalMoves = getLegalMoves(color);
        return legalMoves.isEmpty();
    }

    public boolean isStalemate(PieceColor color) {
        if (isInCheck(color)) {
            return false;
        }

        List<Move> legalMoves = getLegalMoves(color);
        return legalMoves.isEmpty();
    }

    public Move getLastMove() {
        if (moveHistory.isEmpty()) {
            return null;
        }
        return moveHistory.get(moveHistory.size() - 1);
    }

    // NEW: Get move history method needed by ChessController
    public List<Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    public boolean makeMove(Move move) {
        Position from = move.getFrom();
        Position to = move.getTo();
        Piece piece = move.getPiece();

        // Handle en passant capture (captured piece is not at the destination)
        if (move.isEnPassant()) {
            Position capturedPos = move.getCapturedPiecePosition();
            Piece capturedPiece = getPiece(capturedPos);

            // Remove the captured pawn
            pieces[capturedPos.getY()][capturedPos.getX()] = null;
            if (capturedPiece.getColor() == PieceColor.WHITE) {
                whitePieces.remove(capturedPiece);
            } else {
                blackPieces.remove(capturedPiece);
            }
        }
        // Handle regular capture
        else if (move.getCapturedPiece() != null) {
            Piece capturedPiece = move.getCapturedPiece();
            if (capturedPiece.getColor() == PieceColor.WHITE) {
                whitePieces.remove(capturedPiece);
            } else {
                blackPieces.remove(capturedPiece);
            }
        }

        // Handle castling (move the rook as well)
        if (move.isCastling()) {
            Rook rook = move.getCastlingRook();
            Position rookFrom = move.getRookFromPosition();
            Position rookTo = move.getRookToPosition();

            // Move the rook
            pieces[rookFrom.getY()][rookFrom.getX()] = null;
            pieces[rookTo.getY()][rookTo.getX()] = rook;
            rook.setPosition(rookTo);
            rook.setHasMoved(true);
        }

        // Update piece position
        pieces[from.getY()][from.getX()] = null;

        // Handle promotion
        if (move.isPromotion()) {
            // Create the promoted piece
            Piece promotedPiece;
            String promotionType = move.getPromotionType();
            PieceColor color = piece.getColor();

            switch (promotionType) {
                case "Queen":
                    promotedPiece = new Queen(color, to);
                    break;
                case "Rook":
                    promotedPiece = new Rook(color, to);
                    break;
                case "Bishop":
                    promotedPiece = new Bishop(color, to);
                    break;
                case "Knight":
                    promotedPiece = new Knight(color, to);
                    break;
                default:
                    promotedPiece = new Queen(color, to); // Default to queen
            }

            // Update the board and pieces list
            pieces[to.getY()][to.getX()] = promotedPiece;

            // Remove the pawn from the pieces list
            if (color == PieceColor.WHITE) {
                whitePieces.remove(piece);
                whitePieces.add(promotedPiece);
            } else {
                blackPieces.remove(piece);
                blackPieces.add(promotedPiece);
            }
        } else {
            // Normal move
            pieces[to.getY()][to.getX()] = piece;
            piece.setPosition(to);

            // Mark piece as moved
            if (piece instanceof Pawn) {
                ((Pawn) piece).setHasMoved(true);
            } else if (piece instanceof King) {
                ((King) piece).setHasMoved(true);
            } else if (piece instanceof Rook) {
                ((Rook) piece).setHasMoved(true);
            }
        }

        // Add to move history
        moveHistory.add(move);
        return true;
    }

    public void removePiece(Position position) {
        if (!isValidPosition(position)) {
            return;
        }

        Piece piece = getPiece(position);
        if (piece != null) {
            if (piece.getColor() == PieceColor.WHITE) {
                whitePieces.remove(piece);
            } else {
                blackPieces.remove(piece);
            }
            pieces[position.getY()][position.getX()] = null;
        }
    }
}