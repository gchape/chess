package io.gchape.github.view;

import io.gchape.github.controller.ChessController;
import io.gchape.github.model.*;
import io.gchape.github.model.entity.ClientMode;
import javafx.scene.layout.GridPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;
import javafx.geometry.Insets;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import java.util.List;

/**
 * UI component that renders the chess board and handles user interactions with it.
 * Updated for network play with client mode support - JavaFX version.
 */
public class ChessBoardUI extends GridPane {
    private static final int BOARD_SIZE = 8;
    private static final int SQUARE_SIZE = 75; // Size of each square in pixels

    private ChessController controller;
    private SquareView[][] squares;
    private Position selectedPosition;
    private List<Position> legalMovePositions;
    private ClientMode clientMode = ClientMode.PLAYER; // Default to PLAYER mode
    private PieceColor playerColor = PieceColor.WHITE; // Default player color
    private boolean isNetworkGame = false;

    /**
     * Constructs a ChessBoardUI with the given controller.
     *
     * @param controller The chess game controller
     */
    public ChessBoardUI(ChessController controller) {
        this.controller = controller;
        this.selectedPosition = null;
        this.legalMovePositions = null;

        // Set layout and appearance
        setupGridPane();

        // Create the chess board squares
        initializeBoard();
    }

    /**
     * Sets up the GridPane properties and styling.
     */
    private void setupGridPane() {
        // Set padding and border
        setPadding(new Insets(0));
        setBorder(new Border(new BorderStroke(
                Color.BLACK,
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(2)
        )));

        // Set preferred size
        setPrefSize(BOARD_SIZE * SQUARE_SIZE, BOARD_SIZE * SQUARE_SIZE);
        setMaxSize(BOARD_SIZE * SQUARE_SIZE, BOARD_SIZE * SQUARE_SIZE);
        setMinSize(BOARD_SIZE * SQUARE_SIZE, BOARD_SIZE * SQUARE_SIZE);

        // Remove gaps between grid cells
        setHgap(0);
        setVgap(0);
    }

    /**
     * Initialize the chess board with square views.
     */
    private void initializeBoard() {
        squares = new SquareView[BOARD_SIZE][BOARD_SIZE];

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boolean isLight = (row + col) % 2 == 0;

                Position position = new Position(col, row);
                squares[row][col] = new SquareView(position, isLight);

                // Set square size
                squares[row][col].setPrefSize(SQUARE_SIZE, SQUARE_SIZE);
                squares[row][col].setMaxSize(SQUARE_SIZE, SQUARE_SIZE);
                squares[row][col].setMinSize(SQUARE_SIZE, SQUARE_SIZE);

                // Add click listener to the square
                squares[row][col].setOnMouseClicked(this::handleSquareMouseClicked);
                squares[row][col].setOnMouseEntered(this::handleSquareMouseEntered);
                squares[row][col].setOnMouseExited(this::handleSquareMouseExited);

                // Add to grid
                add(squares[row][col], col, row);
            }
        }
    }

    /**
     * Sets the client mode (PLAYER or SPECTATOR).
     *
     * @param mode The client mode
     */
    public void setClientMode(ClientMode mode) {
        this.clientMode = mode;
        // Disable interactions for spectators
        setDisable(mode != ClientMode.PLAYER);
    }

    /**
     * Sets the player's color for network games.
     *
     * @param color The player's piece color
     */
    public void setPlayerColor(PieceColor color) {
        this.playerColor = color;
    }

    /**
     * Sets whether this is a network game.
     *
     * @param isNetwork True if network game, false for local game
     */
    public void setNetworkGame(boolean isNetwork) {
        this.isNetworkGame = isNetwork;
    }

    /**
     * Updates the board UI to reflect the current game state.
     */
    public void updateBoard() {
        Board board = controller.getBoard();

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Position pos = new Position(col, row);
                Piece piece = board.getPiece(pos);
                squares[row][col].setOccupyingPiece(piece);

                // Check if this square is selected
                boolean isSelected = (selectedPosition != null &&
                        selectedPosition.getX() == col &&
                        selectedPosition.getY() == row);
                squares[row][col].setSelected(isSelected);

                // Check if this square is a legal move
                boolean isLegalMove = false;
                if (legalMovePositions != null) {
                    for (Position move : legalMovePositions) {
                        if (move != null && move.getX() == col && move.getY() == row) {
                            isLegalMove = true;
                            break;
                        }
                    }
                }
                squares[row][col].setLegalMove(isLegalMove);
            }
        }
    }

    /**
     * Clears the current selection and legal move highlights.
     */
    public void clearSelection() {
        selectedPosition = null;
        legalMovePositions = null;
        updateBoard();
    }

    /**
     * Handles mouse click events on chess squares.
     *
     * @param event The mouse event
     */
    private void handleSquareMouseClicked(MouseEvent event) {
        if (!isEnabled()) {
            return;
        }

        SquareView sourceSquare = (SquareView) event.getSource();
        Position position = sourceSquare.getPosition();
        handleSquareClick(position);
    }

    /**
     * Handles mouse enter events on chess squares.
     *
     * @param event The mouse event
     */
    private void handleSquareMouseEntered(MouseEvent event) {
        if (isEnabled()) {
            SquareView sourceSquare = (SquareView) event.getSource();
            sourceSquare.setCursor(Cursor.HAND);
        }
    }

    /**
     * Handles mouse exit events on chess squares.
     *
     * @param event The mouse event
     */
    private void handleSquareMouseExited(MouseEvent event) {
        SquareView sourceSquare = (SquareView) event.getSource();
        sourceSquare.setCursor(Cursor.DEFAULT);
    }

    /**
     * Handles a click on a chess square.
     *
     * @param position The position of the clicked square
     */
    private void handleSquareClick(Position position) {
        // If game is over, do nothing
        if (controller.isGameOver()) {
            return;
        }

        // If spectator mode, don't allow moves
        if (clientMode == ClientMode.SPECTATOR) {
            return;
        }

        // For network games, only allow moves when it's the player's turn
        if (isNetworkGame && controller.getCurrentTurn() != playerColor) {
            return;
        }

        // Get the piece at the clicked position
        Piece clickedPiece = controller.getPieceAt(position);

        // If a piece is already selected
        if (selectedPosition != null) {
            // If clicking the same piece, deselect it
            if (position.equals(selectedPosition)) {
                selectedPosition = null;
                legalMovePositions = null;
                updateBoard();
                return;
            }

            // Check if the clicked position is a legal move
            boolean isLegalMove = false;
            if (legalMovePositions != null) {
                for (Position movePos : legalMovePositions) {
                    if (movePos.equals(position)) {
                        isLegalMove = true;
                        break;
                    }
                }
            }

            // If it's a legal move, make the move
            if (isLegalMove) {
                boolean moveSuccessful = controller.makeMove(selectedPosition, position);
                if (moveSuccessful) {
                    selectedPosition = null;
                    legalMovePositions = null;
                    updateBoard();

                    // For network games, the move will be sent over the network
                    // The controller handles network communication
                }
                return;
            }

            // If clicking another piece of the same color, select that piece instead
            PieceColor currentPlayerColor = isNetworkGame ? playerColor : controller.getCurrentTurn();
            if (clickedPiece != null && clickedPiece.getColor() == currentPlayerColor) {
                selectedPosition = position;
                legalMovePositions = controller.getLegalMovePositions(position);
                updateBoard();
                return;
            }

            // Otherwise, deselect
            selectedPosition = null;
            legalMovePositions = null;
            updateBoard();
            return;
        }

        // If no piece is selected and a piece of the current player's color is clicked
        PieceColor currentPlayerColor = isNetworkGame ? playerColor : controller.getCurrentTurn();
        if (clickedPiece != null && clickedPiece.getColor() == currentPlayerColor) {
            selectedPosition = position;
            legalMovePositions = controller.getLegalMovePositions(position);
            updateBoard();
        }
    }

    /**
     * Handles a network move received from the server.
     * This method is called when a move is received over the network.
     *
     * @param from The source position
     * @param to The destination position
     */
    public void handleNetworkMove(Position from, Position to) {
        // Clear any current selection
        selectedPosition = null;
        legalMovePositions = null;

        // Update the board display
        updateBoard();
    }

    /**
     * Checks if the board is enabled for interaction.
     *
     * @return True if enabled, false if disabled
     */
    public boolean isEnabled() {
        return !isDisabled();
    }

    /**
     * Disables/enables board interaction.
     *
     * @param enabled True to enable interaction, false to disable
     */
    public void setEnabled(boolean enabled) {
        setDisable(!enabled);

        // Enable/disable all square interactions
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                squares[row][col].setDisable(!enabled);
            }
        }
    }
}