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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UIManager {
    private static final int GAME_WINDOW_WIDTH = 800;
    private static final int GAME_WINDOW_HEIGHT = 800;

    private final ClientView clientView;

    public UIManager(ClientView clientView) {
        this.clientView = clientView;
    }

    public void switchToGameView(MouseEvent e) {
        Platform.runLater(() -> {
            Stage stage = getStageFromEvent(e);
            Scene scene = stage.getScene();

            scene.setRoot(clientView.createBoard());
            stage.setWidth(GAME_WINDOW_WIDTH);
            stage.setHeight(GAME_WINDOW_HEIGHT);
        });
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
        highlightSelectedSquare(position);
        highlightValidMoves(validMoves);
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
    }

    public void updateBoard(GameState gameState) {
        clientView.updateBoard(gameState);
    }

    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private StackPane getSquare(Position position) {
        for (Node node : clientView.getBoard().getChildren()) {
            if (node instanceof StackPane square) {
                Position pos = (Position) square.getUserData();
                if (pos.equals(position)) {
                    return square;
                }
            }
        }
        return null;
    }
}
