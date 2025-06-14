package io.gchape.github.view;

import io.gchape.github.model.*;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import java.io.InputStream;

/**
 * Represents a single square on the chess board UI using JavaFX.
 * Uses only basic JavaFX imports for maximum compatibility.
 */
public class SquareView extends StackPane {
    private static final Color LIGHT_SQUARE_COLOR = Color.rgb(240, 217, 181);
    private static final Color DARK_SQUARE_COLOR = Color.rgb(181, 136, 99);
    private static final Color SELECTED_SQUARE_COLOR = Color.rgb(130, 151, 105);
    private static final Color LEGAL_MOVE_COLOR = Color.rgb(130, 151, 105, 0.5);

    private final Position position;
    private final boolean isLightSquare;
    private final Rectangle background;
    private final Rectangle overlay;
    private final ImageView pieceImageView;
    private final Text coordinateText;
    private final Text fallbackText;

    private Piece occupyingPiece;
    private boolean isSelected;
    private boolean isLegalMove;

    /**
     * Constructs a new SquareView at the specified position.
     *
     * @param position The board position this square represents
     * @param isLightSquare Whether this is a light-colored square
     */
    public SquareView(Position position, boolean isLightSquare) {
        this.position = position;
        this.isLightSquare = isLightSquare;
        this.occupyingPiece = null;
        this.isSelected = false;
        this.isLegalMove = false;

        // Create background rectangle
        this.background = new Rectangle(75, 75);
        this.background.setFill(isLightSquare ? LIGHT_SQUARE_COLOR : DARK_SQUARE_COLOR);

        // Create overlay rectangle for highlighting
        this.overlay = new Rectangle(75, 75);
        this.overlay.setFill(Color.TRANSPARENT);

        // Create image view for piece
        this.pieceImageView = new ImageView();
        this.pieceImageView.setFitWidth(60);
        this.pieceImageView.setFitHeight(60);
        this.pieceImageView.setPreserveRatio(true);
        this.pieceImageView.setVisible(false);

        // Create text for coordinates (small text in corner)
        this.coordinateText = new Text(position.toString());
        this.coordinateText.setFont(Font.font("Arial", 8));
        this.coordinateText.setFill(isLightSquare ? DARK_SQUARE_COLOR : LIGHT_SQUARE_COLOR);
        this.coordinateText.setTranslateX(-30);
        this.coordinateText.setTranslateY(-30);

        // Create fallback text for piece representation
        this.fallbackText = new Text();
        this.fallbackText.setFont(Font.font("Arial", 20));
        this.fallbackText.setStyle("-fx-font-weight: bold;");
        this.fallbackText.setVisible(false);

        // Add all elements to the stack pane
        getChildren().addAll(background, overlay, pieceImageView, fallbackText, coordinateText);

        setPrefSize(75, 75);
        setMaxSize(75, 75);
        setMinSize(75, 75);
    }

    /**
     * Sets the piece occupying this square.
     *
     * @param piece The piece to place on this square, or null to remove
     */
    public void setOccupyingPiece(Piece piece) {
        this.occupyingPiece = piece;
        updatePieceDisplay();
    }

    /**
     * Gets the piece occupying this square.
     *
     * @return The occupying piece, or null if empty
     */
    public Piece getOccupyingPiece() {
        return occupyingPiece;
    }

    /**
     * Checks if this square is occupied by a piece.
     *
     * @return True if occupied, false otherwise
     */
    public boolean isOccupied() {
        return occupyingPiece != null;
    }

    /**
     * Sets whether this square is selected.
     *
     * @param selected True if selected, false otherwise
     */
    public void setSelected(boolean selected) {
        this.isSelected = selected;
        updateSquareAppearance();
    }

    /**
     * Sets whether this square represents a legal move.
     *
     * @param legalMove True if a legal move, false otherwise
     */
    public void setLegalMove(boolean legalMove) {
        this.isLegalMove = legalMove;
        updateSquareAppearance();
    }

    /**
     * Gets the position this square represents.
     *
     * @return The board position
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Updates the visual appearance of the square based on its state.
     */
    private void updateSquareAppearance() {
        if (isSelected) {
            background.setFill(SELECTED_SQUARE_COLOR);
            overlay.setFill(Color.TRANSPARENT);
        } else if (isLegalMove) {
            background.setFill(isLightSquare ? LIGHT_SQUARE_COLOR : DARK_SQUARE_COLOR);
            overlay.setFill(LEGAL_MOVE_COLOR);
        } else {
            background.setFill(isLightSquare ? LIGHT_SQUARE_COLOR : DARK_SQUARE_COLOR);
            overlay.setFill(Color.TRANSPARENT);
        }
    }

    /**
     * Updates the piece display based on the current occupying piece.
     */
    private void updatePieceDisplay() {
        if (occupyingPiece == null) {
            pieceImageView.setVisible(false);
            fallbackText.setVisible(false);
            return;
        }

        // Try to load the piece image
        Image pieceImage = loadPieceImage(occupyingPiece.getImageFile());

        if (pieceImage != null) {
            pieceImageView.setImage(pieceImage);
            pieceImageView.setVisible(true);
            fallbackText.setVisible(false);
        } else {
            // Show fallback text representation
            pieceImageView.setVisible(false);
            showFallbackPiece();
        }
    }

    /**
     * Attempts to load the piece image using multiple methods.
     *
     * @param imageFileName The image file name
     * @return The loaded image, or null if all loading methods failed
     */
    private Image loadPieceImage(String imageFileName) {
        if (imageFileName == null || imageFileName.isEmpty()) {
            return null;
        }

        try {
            // Method 1: Try to load from class resources
            InputStream resourceStream = getClass().getResourceAsStream("/images/" + imageFileName);
            if (resourceStream != null) {
                return new Image(resourceStream);
            }
        } catch (Exception e) {
            System.err.println("Failed to load from class resources: " + e.getMessage());
        }

        try {
            // Method 2: Try to load from classpath root resources
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("images/" + imageFileName);
            if (resourceStream != null) {
                return new Image(resourceStream);
            }
        } catch (Exception e) {
            System.err.println("Failed to load from classpath root: " + e.getMessage());
        }

        try {
            // Method 3: Try to load from file system using file:// URL
            String[] possiblePaths = {
                    "file:resources/images/" + imageFileName,
                    "file:src/main/resources/images/" + imageFileName,
                    "file:src/resources/images/" + imageFileName,
                    "file:images/" + imageFileName
            };

            for (String path : possiblePaths) {
                try {
                    return new Image(path);
                } catch (Exception e) {
                    // Continue to next path
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load from file system: " + e.getMessage());
        }

        return null;
    }

    /**
     * Shows a fallback representation of the chess piece when image loading fails.
     */
    private void showFallbackPiece() {
        if (occupyingPiece == null) return;

        String pieceSymbol = getPieceSymbol(occupyingPiece);
        fallbackText.setText(pieceSymbol);

        // Set color based on piece color
        if (occupyingPiece.getColor().toString().equals("WHITE")) {
            fallbackText.setFill(Color.WHITE);
            fallbackText.setStroke(Color.BLACK);
            fallbackText.setStrokeWidth(1);
        } else {
            fallbackText.setFill(Color.BLACK);
            fallbackText.setStroke(Color.WHITE);
            fallbackText.setStrokeWidth(0.5);
        }

        fallbackText.setVisible(true);
    }

    /**
     * Gets a Unicode symbol representation of the chess piece.
     *
     * @param piece The chess piece
     * @return Unicode symbol for the piece
     */
    private String getPieceSymbol(Piece piece) {
        String pieceType = piece.getType().toUpperCase();
        boolean isWhite = piece.getColor().toString().equals("WHITE");

        switch (pieceType) {
            case "KING":
                return isWhite ? "♔" : "♚";
            case "QUEEN":
                return isWhite ? "♕" : "♛";
            case "ROOK":
                return isWhite ? "♖" : "♜";
            case "BISHOP":
                return isWhite ? "♗" : "♝";
            case "KNIGHT":
                return isWhite ? "♘" : "♞";
            case "PAWN":
                return isWhite ? "♙" : "♟";
            default:
                return pieceType.substring(0, 1);
        }
    }
}