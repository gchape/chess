package io.gchape.github.model.service;

import io.gchape.github.model.GameState;
import io.gchape.github.model.entity.Position;
import io.gchape.github.view.ClientView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UIManager {
    private static final Logger logger = LoggerFactory.getLogger(UIManager.class);
    private static final int GAME_WINDOW_WIDTH = 800;
    private static final int GAME_WINDOW_HEIGHT = 800;

    private final ClientView clientView;

    public UIManager(ClientView clientView) {
        this.clientView = clientView;
    }

    public void switchToGameView(MouseEvent e) {
        try {
            Stage stage = getStageFromEvent(e);
            Scene scene = stage.getScene();

            scene.setRoot(clientView.createBoard());
            stage.setWidth(GAME_WINDOW_WIDTH);
            stage.setHeight(GAME_WINDOW_HEIGHT);

            logger.info("Switched to game view");
        } catch (Exception ex) {
            logger.error("Error switching to game view", ex);
            showError("Failed to switch to game view: " + ex.getMessage());
        }
    }

    private Stage getStageFromEvent(MouseEvent e) {
        Scene scene = ((Node) e.getSource()).getScene();
        return (Stage) scene.getWindow();
    }

    public Position getPositionFromEvent(MouseEvent event) {
        Node source = (Node) event.getSource();
        if (source instanceof StackPane square) {
            return (Position) square.getUserData();
        }
        return null;
    }

    public void highlightSelection(Position position, List<Position> validMoves) {
        ensureFxThread(() -> {
            highlightSelectedSquare(position);
            highlightValidMoves(validMoves);
        });
    }

    private void highlightSelectedSquare(Position position) {
        StackPane square = getSquare(position);
        if (square != null) {
            square.getStyleClass().add("highlight-pressed");
        }
    }

    private void highlightValidMoves(List<Position> validMoves) {
        for (Position move : validMoves) {
            StackPane square = getSquare(move);
            if (square != null) {
                square.getStyleClass().add("highlight-moves");
                clientView.highlightedSquares.add(square);
            }
        }
    }

    public void clearHighlights() {
        ensureFxThread(() -> {
            if (clientView.selectedPosition != null) {
                StackPane selectedSquare = getSquare(clientView.selectedPosition);
                if (selectedSquare != null) {
                    selectedSquare.getStyleClass().remove("highlight-pressed");
                }
            }

            clientView.highlightedSquares.forEach(square ->
                    square.getStyleClass().remove("highlight-moves"));
            clientView.highlightedSquares.clear();
            clientView.selectedPosition = null;
        });
    }

    public void updateBoard(GameState gameState) {
        Platform.runLater(() -> {
            try {
                clientView.updateBoard(gameState);
                logger.debug("Board updated with new game state");
            } catch (Exception e) {
                logger.error("Error updating board", e);
                showError("Failed to update board: " + e.getMessage());
            }
        });
    }

    public void showError(String message) {
        ensureFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            logger.warn("Showed error dialog: {}", message);
        });
    }

    public void showSuccess(String message) {
        ensureFxThread(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            logger.info("Showed success dialog: {}", message);
        });
    }

    private StackPane getSquare(Position position) {
        for (Node node : clientView.getBoard().getChildren()) {
            if (node instanceof StackPane square) {
                Position pos = (Position) square.getUserData();
                if (pos != null && pos.equals(position)) {
                    return square;
                }
            }
        }
        return null;
    }

    /**
     * Ensures the action runs on the JavaFX Application Thread
     */
    private void ensureFxThread(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}