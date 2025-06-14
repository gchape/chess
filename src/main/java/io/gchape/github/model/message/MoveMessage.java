package io.gchape.github.model.message;

public class MoveMessage extends Message {
    private String type = "MOVE";

    private String fromPosition;
    private String toPosition;
    private String pieceType;
    private String promotionPiece;
    private Long gameId;
    private Long userId; // Added to match ClientController usage

    public MoveMessage() {}

    public MoveMessage(String fromPosition, String toPosition, String pieceType, Long gameId) {
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
        this.pieceType = pieceType;
        this.gameId = gameId;
    }

    public MoveMessage(String fromPosition, String toPosition, String pieceType, Long gameId, Long userId) {
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
        this.pieceType = pieceType;
        this.gameId = gameId;
        this.userId = userId;
    }

    @Override
    public String getType() { return "MOVE"; }

    // Dual method names to support both ClientController and Server usage
    public String getFromSquare() { return fromPosition; }
    public String getFromPosition() { return fromPosition; }
    public void setFromSquare(String fromSquare) { this.fromPosition = fromSquare; }
    public void setFromPosition(String fromPosition) { this.fromPosition = fromPosition; }

    public String getToSquare() { return toPosition; }
    public String getToPosition() { return toPosition; }
    public void setToSquare(String toSquare) { this.toPosition = toSquare; }
    public void setToPosition(String toPosition) { this.toPosition = toPosition; }

    public String getPieceType() { return pieceType; }
    public void setPieceType(String pieceType) { this.pieceType = pieceType; }

    public String getPromotion() { return promotionPiece; }
    public String getPromotionPiece() { return promotionPiece; }
    public void setPromotion(String promotion) { this.promotionPiece = promotion; }
    public void setPromotionPiece(String promotionPiece) { this.promotionPiece = promotionPiece; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    // Added userId support
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
