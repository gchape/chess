package io.gchape.github.model;



public enum PieceColor {
    WHITE(1),
    BLACK(0);

    private final int value;

    PieceColor(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PieceColor fromValue(int value) {
        for (PieceColor color : values()) {
            if (color.getValue() == value) {
                return color;
            }
        }
        throw new IllegalArgumentException("Invalid PieceColor value: " + value);
    }
}
