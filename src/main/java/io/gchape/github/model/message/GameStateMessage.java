package io.gchape.github.model.message;

import java.util.List;

public class GameStateMessage extends Message {
    private String type = "GAME_STATE";

    private Long gameId;
    private String boardState;
    private String currentTurn;
    private int moveNumber;
    private boolean gameEnded;
    private Long whitePlayerId;
    private Long blackPlayerId;
    private List<Long> spectators; // Fixed generic type

    // New fields for enhanced game state
    private boolean check;
    private boolean checkmate;
    private boolean stalemate;
    private String gameResult;

    public GameStateMessage() {}

    @Override
    public String getType() {
        return "GAME_STATE";
    }

    // Original getters and setters maintained
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    public String getBoardState() { return boardState; }
    public void setBoardState(String boardState) { this.boardState = boardState; }

    public String getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(String currentTurn) { this.currentTurn = currentTurn; }

    public int getMoveNumber() { return moveNumber; }
    public void setMoveNumber(int moveNumber) { this.moveNumber = moveNumber; }

    public boolean isGameEnded() { return gameEnded; }
    public void setGameEnded(boolean gameEnded) { this.gameEnded = gameEnded; }

    public Long getWhitePlayerId() { return whitePlayerId; }
    public void setWhitePlayerId(Long whitePlayerId) { this.whitePlayerId = whitePlayerId; }

    public Long getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(Long blackPlayerId) { this.blackPlayerId = blackPlayerId; }

    public List<Long> getSpectators() { return spectators; }
    public void setSpectators(List<Long> spectators) { this.spectators = spectators; }

    // New getters and setters for enhanced fields
    public boolean isCheck() { return check; }
    public void setCheck(boolean check) { this.check = check; }

    public boolean isCheckmate() { return checkmate; }
    public void setCheckmate(boolean checkmate) { this.checkmate = checkmate; }

    public boolean isStalemate() { return stalemate; }
    public void setStalemate(boolean stalemate) { this.stalemate = stalemate; }

    public String getGameResult() { return gameResult; }
    public void setGameResult(String gameResult) { this.gameResult = gameResult; }
}