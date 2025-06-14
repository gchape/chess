package io.gchape.github.model.message;

public class SpectateMessage extends Message {
    private Long gameId;
    private String type = "SPECTATE";

    private Long userId; // Added to match ClientController usage

    public SpectateMessage() {}

    public SpectateMessage(Long gameId) {
        this.gameId = gameId;
    }

    public SpectateMessage(Long gameId, Long userId) {
        this.gameId = gameId;
        this.userId = userId;
    }

    @Override
    public String getType() { return "SPECTATE"; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    // Added userId support
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}