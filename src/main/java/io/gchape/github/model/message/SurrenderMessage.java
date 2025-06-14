package io.gchape.github.model.message;

public class SurrenderMessage extends Message {
    private Long gameId;
    private String type ="SURRENDER";
    private Long userId;

    public SurrenderMessage() {}

    public SurrenderMessage(Long gameId, Long userId) {
        this.gameId = gameId;
        this.userId = userId;
    }

    @Override
    public String getType() { return "SURRENDER"; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}