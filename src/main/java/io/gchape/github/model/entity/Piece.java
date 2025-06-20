package io.gchape.github.model.entity;

public enum Piece {
    WP("wp", 'w'), WR("wr", 'w'), WN("wn", 'w'),
    WB("wb", 'w'), WQ("wq", 'w'), WK("wk", 'w'),
    BP("bp", 'b'), BR("br", 'b'), BN("bn", 'b'),
    BB("bb", 'b'), BQ("bq", 'b'), BK("bk", 'b');

    public final char color;
    private final String code;

    Piece(final String code, final char color) {
        this.code = code;
        this.color = color;
    }

    public static Piece fromCode(final String code) {
        for (Piece piece : values()) {
            if (piece.code.equals(code)) {
                return piece;
            }
        }
        throw new IllegalArgumentException("Invalid piece code: " + code);
    }

    public String imageName() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}
