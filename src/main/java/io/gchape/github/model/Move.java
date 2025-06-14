package io.gchape.github.model;


import io.gchape.github.model.piece.Rook;

public class Move {
    private final Position from;
    private final Position to;
    private final Piece piece;
    private final Piece capturedPiece;
    private final boolean isPromotion;
    private final boolean isCastling;
    private final boolean isEnPassant;
    private final boolean isPawnTwoSquareMove;
    private final Rook castlingRook;
    private final Position rookFromPosition;
    private final String promotionType;

    private final Position rookToPosition;
    private final Position capturedPiecePosition;

    private Move(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.piece = builder.piece;
        this.capturedPiece = builder.capturedPiece;
        this.isPromotion = builder.isPromotion;
        this.isCastling = builder.isCastling;
        this.isEnPassant = builder.isEnPassant;
        this.promotionType = builder.promotionType;

        this.isPawnTwoSquareMove = builder.isPawnTwoSquareMove;
        this.castlingRook = builder.castlingRook;
        this.rookFromPosition = builder.rookFromPosition;
        this.rookToPosition = builder.rookToPosition;
        this.capturedPiecePosition = builder.capturedPiecePosition;
    }

    public Position getFrom() {
        return from;
    }

    public Position getTo() {
        return to;
    }

    public Piece getPiece() {
        return piece;
    }

    public Piece getCapturedPiece() {
        return capturedPiece;
    }
    public String getPromotionType() {
        return promotionType;
    }


    public boolean isPromotion() {
        return isPromotion;
    }

    public boolean isCastling() {
        return isCastling;
    }

    public boolean isEnPassant() {
        return isEnPassant;
    }

    public boolean isPawnTwoSquareMove() {
        return isPawnTwoSquareMove;
    }

    public Rook getCastlingRook() {
        return castlingRook;
    }

    public Position getRookFromPosition() {
        return rookFromPosition;
    }

    public Position getRookToPosition() {
        return rookToPosition;
    }

    public Position getCapturedPiecePosition() {
        return capturedPiecePosition;
    }

    // Builder pattern for Move construction
    public static class Builder {
        private Position from;
        private Position to;
        private Piece piece;
        private Piece capturedPiece;
        private boolean isPromotion;
        private String promotionType;

        private boolean isCastling;
        private boolean isEnPassant;
        private boolean isPawnTwoSquareMove = false;
        private Rook castlingRook;
        private Position rookFromPosition;
        private Position rookToPosition;
        private Position capturedPiecePosition;


        public Builder from(Position from) {
            this.from = from;
            return this;
        }

        public Builder to(Position to) {
            this.to = to;
            return this;
        }

        public Builder piece(Piece piece) {
            this.piece = piece;
            return this;
        }

        public Builder capturedPiece(Piece capturedPiece) {
            this.capturedPiece = capturedPiece;
            return this;
        }

        public Builder isPromotion(boolean isPromotion) {
            this.isPromotion = isPromotion;
            return this;
        }
        public Builder promotionType(String promotionType) {
            this.promotionType = promotionType;
            return this;
        }

        public Builder isCastling(boolean isCastling) {
            this.isCastling = isCastling;
            return this;
        }
        public Builder castlingRook(Rook castlingRook) {
            this.castlingRook = castlingRook;
            return this;
        }

        public Builder rookFromPosition(Position rookFromPosition) {
            this.rookFromPosition = rookFromPosition;
            return this;
        }

        public Builder rookToPosition(Position rookToPosition) {
            this.rookToPosition = rookToPosition;
            return this;
        }

        public Builder capturedPiecePosition(Position capturedPiecePosition) {
            this.capturedPiecePosition = capturedPiecePosition;
            return this;
        }
        public Builder isEnPassant(boolean isEnPassant) {
            this.isEnPassant = isEnPassant;
            return this;
        }

        /**
         * Determines if this move was a pawn's two-square advance
         * @return true if this move represents a pawn moving two squares, false otherwise
         */
        public Builder isPawnTwoSquareMove(boolean b) {
            this.isPawnTwoSquareMove = b;
            return this;
        }

        public Move build() {
            return new Move(this);
        }

        /**
         * Determines if this move was a pawn's two-square advance
         * @return true if this move represents a pawn moving two squares, false otherwise
         */

    }
}
