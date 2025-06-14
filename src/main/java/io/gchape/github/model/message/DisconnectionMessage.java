package io.gchape.github.model.message;

public class DisconnectionMessage extends Message {
    private String type = "DISCONNECTION";

    private Long gameId;
    private Long disconnectedUserId;
    private Long userId; // Added to match ClientController usage (can be same as disconnectedUserId)

    public DisconnectionMessage() {}

    public DisconnectionMessage(Long gameId, Long disconnectedUserId) {
        this.gameId = gameId;
        this.disconnectedUserId = disconnectedUserId;
        this.userId = disconnectedUserId; // Default to same user
    }

    public DisconnectionMessage(Long gameId, Long disconnectedUserId, Long userId) {
        this.gameId = gameId;
        this.disconnectedUserId = disconnectedUserId;
        this.userId = userId;
    }

    @Override
    public String getType() { return "PLAYER_DISCONNECTED"; }

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public Long getDisconnectedUserId() { return disconnectedUserId; }
    public void setDisconnectedUserId(Long disconnectedUserId) { this.disconnectedUserId = disconnectedUserId; }

    // Added userId support
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
