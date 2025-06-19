package io.gchape.github.model.entity;

public record Position(int row, int col) {

    public static Position parse(String notation) {
        if (notation == null) {
            throw new IllegalArgumentException("Position notation cannot be null");
        }

        String cleaned = notation.replaceAll("[\\(\\)]", "").trim();
        String[] parts = cleaned.split(",");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid position notation: " + notation);
        }

        try {
            int row = Integer.parseInt(parts[0].trim());
            int col = Integer.parseInt(parts[1].trim());
            return new Position(row, col);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid position coordinates: " + notation);
        }
    }

    public boolean isInBounds() {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    @Override
    public String toString() {
        return "(" + row + "," + col + ")";
    }
}