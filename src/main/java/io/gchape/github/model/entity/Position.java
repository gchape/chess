package io.gchape.github.model.entity;

public record Position(int row, int col) {

    public boolean isInBounds() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}