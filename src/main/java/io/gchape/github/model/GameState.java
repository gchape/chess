package io.gchape.github.model;

import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;

public class GameState {
    private static final int BOARD_SIZE = 8;
    private final Piece[][] board = new Piece[BOARD_SIZE][BOARD_SIZE];
    private boolean isWhite;

    public GameState() {
        initializeBoard();
    }

    private void initializeBoard() {
        final Piece[][] initialSetup = {
                {Piece.BR, Piece.BN, Piece.BB, Piece.BQ, Piece.BK, Piece.BB, Piece.BN, Piece.BR},
                {Piece.BP, Piece.BP, Piece.BP, Piece.BP, Piece.BP, Piece.BP, Piece.BP, Piece.BP},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {Piece.WP, Piece.WP, Piece.WP, Piece.WP, Piece.WP, Piece.WP, Piece.WP, Piece.WP},
                {Piece.WR, Piece.WN, Piece.WB, Piece.WQ, Piece.WK, Piece.WB, Piece.WN, Piece.WR},
        };

        for (int row = 0; row < BOARD_SIZE; row++) {
            System.arraycopy(initialSetup[row], 0, board[row], 0, BOARD_SIZE);
        }
    }

    public Piece getPieceAt(final Position position) {
        if (!position.isInBounds()) return null;

        return board[position.row()][position.col()];
    }

    public void setPieceAt(final Position position, final Piece piece) {
        if (position.isInBounds()) {
            board[position.row()][position.col()] = piece;
        }
    }

    public boolean getColor() {
        return isWhite;
    }

    public void setColor(boolean white) {
        isWhite = white;
    }
}
