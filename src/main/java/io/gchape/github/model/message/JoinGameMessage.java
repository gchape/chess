package io.gchape.github.model.message;

public class JoinGameMessage extends Message {
    private String type = "JOIN_GAME";
    private String gameMode;
    private Long userId;

    public JoinGameMessage() {}

    @Override
    public String getType() { return "JOIN_GAME"; }

    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}