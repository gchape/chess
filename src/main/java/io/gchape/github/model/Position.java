package io.gchape.github.model;

public class Position {
    private final int x;
    private final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // New method for algebraic notation support
    public static Position fromAlgebraic(String algebraic) {
        if (algebraic == null || algebraic.length() != 2) {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }

        char file = algebraic.charAt(0);
        char rank = algebraic.charAt(1);

        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            throw new IllegalArgumentException("Invalid algebraic notation: " + algebraic);
        }

        int x = file - 'a';
        int y = rank - '1';

        return new Position(x, y);
    }

    // New method to convert to algebraic notation
    public String toAlgebraic() {
        char file = (char) ('a' + x);
        char rank = (char) ('1' + y);
        return "" + file + rank;
    }

    // Original methods maintained
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}