package io.gchape.github.view;

import io.gchape.github.controller.ClientController;
import io.gchape.github.model.entity.Move;
import io.gchape.github.model.entity.Piece;
import io.gchape.github.model.entity.Position;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.*;

public class ClientView {
    private static final int TILE_SIZE = 80;
    private static final int BOARD_SIZE = 8;
    private static final Color LIGHT_SQUARE = Color.WHEAT;
    private static final Color DARK_SQUARE = Color.SADDLEBROWN;

    private final GridPane board;
    private final BorderPane root;

    private final Map<Piece, Image> pieceImages = new HashMap<>();
    private final List<StackPane> highlightedSquares = new ArrayList<>();

    private ClientController clientController;

    private Position selectedPosition;

    public ClientView() {
        loadPieceImages();
        board = createBoard();

        root = new BorderPane();
        root.setCenter(board);
    }

    public Region view() {
        return root;
    }

    private void loadPieceImages() {
        for (Piece piece : Piece.values()) {
            try {
                var image = new Image(Objects.requireNonNull(
                        getClass().getResourceAsStream("/images/" + piece.imageName() + ".png"))
                );
                pieceImages.put(piece, image);
            } catch (Exception e) {
                System.err.println("Failed to load image for piece: " + piece);
            }
        }
    }

    private GridPane createBoard() {
        var grid = new GridPane();
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                StackPane square = createSquare(row, col);
                grid.add(square, col, row);
            }
        }

        return grid;
    }

    private StackPane createSquare(int row, int col) {
        var rect = new Rectangle(TILE_SIZE, TILE_SIZE);
        var light = (row + col) % 2 == 0;
        rect.setFill(light ? LIGHT_SQUARE : DARK_SQUARE);

        var square = new StackPane();
        square.getChildren().add(rect);
        square.setUserData(new Position(row, col));

        // Only need click handler for simple click-to-move
        square.setOnMouseClicked(this::onSquareClicked);

        return square;
    }

    private void addPieceToSquare(StackPane square, Piece piece) {
        var image = pieceImages.get(piece);
        if (image == null) return;

        var view = new ImageView(image);
        view.setSmooth(true);
        view.setFitWidth(TILE_SIZE * 0.5);
        view.setFitHeight(TILE_SIZE * 0.5);

        square.getChildren().add(view);
    }

    private void removePieceFromSquare(StackPane square) {
        square.getChildren().removeIf(node -> node instanceof ImageView);
    }

    private void updateBoard() {
        // Add null check to prevent NPE
        if (clientController == null) {
            System.out.println("ClientController not set yet, skipping board update");
            return;
        }

        for (Node node : board.getChildren()) {
            if (node instanceof StackPane square) {
                Position pos = (Position) square.getUserData();

                // Clear any existing piece from this square
                removePieceFromSquare(square);

                // Add the current piece if one exists at this position
                Piece piece = clientController.getGameState().getPieceAt(pos);
                if (piece != null) {
                    addPieceToSquare(square, piece);
                }
            }
        }
    }

    private StackPane getSquare(Position position) {
        for (Node node : board.getChildren()) {
            if (node instanceof StackPane square) {
                Position pos = (Position) square.getUserData();
                if (pos.equals(position)) {
                    return square;
                }
            }
        }
        return null;
    }

    private void onSquareClicked(MouseEvent event) {
        // Add null check to prevent NPE during clicks
        if (clientController == null) {
            System.out.println("ClientController not set yet, ignoring click");
            return;
        }

        Node source = (Node) event.getSource();
        if (!(source instanceof StackPane square)) return;

        Position clickedPosition = (Position) square.getUserData();

        // If no piece is currently selected
        if (selectedPosition == null) {
            // Try to select a piece
            if (clientController.canSelectPiece(clickedPosition)) {
                selectPiece(clickedPosition);
            }
        } else {
            // A piece is already selected
            if (clickedPosition.equals(selectedPosition)) {
                // Clicked the same square - deselect
                deselectPiece();
            } else if (clientController.canSelectPiece(clickedPosition)) {
                // Clicked a different piece of the same color - select it instead
                deselectPiece();
                selectPiece(clickedPosition);
            } else {
                // Try to move to the clicked square
                Move move = new Move(selectedPosition, clickedPosition);
                if (clientController.makeMove(move)) {
                    // Move successful
                    updateBoard();
                    deselectPiece();
                    System.out.println("Move made: " + selectedPosition + " -> " + clickedPosition);
                } else {
                    // Invalid move - could add visual feedback here
                    System.out.println("Invalid move: " + selectedPosition + " -> " + clickedPosition);
                }
            }
        }
    }

    private void selectPiece(Position position) {
        // Add null check
        if (clientController == null) return;

        selectedPosition = position;

        // Highlight selected square
        StackPane selectedSquare = getSquare(position);
        if (selectedSquare != null) {
            selectedSquare.getStyleClass().add("highlight-pressed");
        }

        // Show possible moves
        List<Position> validMoves = clientController.getValidMoves(position);
        for (Position move : validMoves) {
            StackPane target = getSquare(move);
            if (target != null) {
                target.getStyleClass().add("highlight-moves");
                highlightedSquares.add(target);
            }
        }

        System.out.println("Selected piece at: " + position + " with " + validMoves.size() + " valid moves");
    }

    private void deselectPiece() {
        // Clear highlights
        if (selectedPosition != null) {
            StackPane selectedSquare = getSquare(selectedPosition);
            if (selectedSquare != null) {
                selectedSquare.getStyleClass().remove("highlight-pressed");
            }
        }

        highlightedSquares.forEach(square -> square.getStyleClass().remove("highlight-moves"));
        highlightedSquares.clear();

        selectedPosition = null;
        System.out.println("Piece deselected");
    }

    public void setClientController(ClientController clientController) {
        this.clientController = clientController;

        // Now that clientController is set, update the board
        updateBoard();

        // Set up the callback for game state changes
        if (clientController != null) {
            clientController.setOnGameStateChanged(this::updateBoard);
        }
    }
}