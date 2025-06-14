package io.gchape.github.model;

public class GameState {
    private Board board;
    private PieceColor currentTurn;
    private boolean gameOver;
    private String gameResult;
    private int moveNumber;

    public GameState() {
        this.board = new Board();
        this.currentTurn = PieceColor.WHITE; // White goes first
        this.gameOver = false;
        this.gameResult = "";
        this.moveNumber = 1;
    }

    // Original methods maintained
    public Board getBoard() {
        return board;
    }

    public PieceColor getCurrentTurn() {
        return currentTurn;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public String getGameResult() {
        return gameResult;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    // NEW: Setter methods needed by ChessController
    public void setGameResult(String gameResult) {
        this.gameResult = gameResult;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void setCurrentTurn(PieceColor currentTurn) {
        this.currentTurn = currentTurn;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    // Original makeMove method maintained
    public boolean makeMove(Move move) {
        // Check if the move is valid
        if (move.getPiece().getColor() != currentTurn) {
            return false;
        }

        // Make the move
        boolean moveSuccessful = board.makeMove(move);
        if (!moveSuccessful) {
            return false;
        }

        // Increment move number
        if (currentTurn == PieceColor.BLACK) {
            moveNumber++;
        }

        // Switch turns
        currentTurn = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;

        // Check for checkmate or stalemate
        checkGameEndConditions();

        return true;
    }

    // Overloaded makeMove method for Position-based moves
    public boolean makeMove(Position from, Position to) {
        Piece piece = board.getPiece(from);
        if (piece == null || piece.getColor() != currentTurn) {
            return false;
        }

        // Create a move object using Builder pattern and delegate to original makeMove
        Move move = new Move.Builder()
                .from(from)
                .to(to)
                .piece(piece)
                .capturedPiece(board.getPiece(to)) // Check if there's a piece to capture
                .build();
        return makeMove(move);
    }

    private void checkGameEndConditions() {
        if (board.isCheckmate(currentTurn)) {
            gameOver = true;
            PieceColor winner = (currentTurn == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
            gameResult = winner.toString() + " wins by checkmate";
        } else if (board.isStalemate(currentTurn)) {
            gameOver = true;
            gameResult = "Draw by stalemate";
        }
    }

    public void resetGame() {
        this.board = new Board();
        this.currentTurn = PieceColor.WHITE;
        this.gameOver = false;
        this.gameResult = "";
        this.moveNumber = 1;
    }
}