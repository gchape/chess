package io.gchape.github.view;

import io.gchape.github.model.entity.db.Game;
import io.gchape.github.model.service.PgnService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PgnDialog {
    private final PgnService pgnService;
    private final Stage parentStage;

    public PgnDialog(PgnService pgnService, Stage parentStage) {
        this.pgnService = pgnService;
        this.parentStage = parentStage;
    }

    /**
     * Show dialog for exporting games to PGN
     */
    public void showExportDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Export Games to PGN");
        dialog.setResizable(false);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // Title
        Label title = new Label("Export Chess Games to PGN");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Export options
        ToggleGroup exportGroup = new ToggleGroup();

        RadioButton singleGameOption = new RadioButton("Export single game");
        singleGameOption.setToggleGroup(exportGroup);
        singleGameOption.setSelected(true);

        RadioButton playerGamesOption = new RadioButton("Export all games by player");
        playerGamesOption.setToggleGroup(exportGroup);

        RadioButton multipleGamesOption = new RadioButton("Export multiple games");
        multipleGamesOption.setToggleGroup(exportGroup);

        // Input fields
        HBox gameIdBox = new HBox(10);
        gameIdBox.getChildren().addAll(
                new Label("Game ID:"),
                createNumberField("gameId", "1")
        );

        HBox playerIdBox = new HBox(10);
        playerIdBox.getChildren().addAll(
                new Label("Player ID:"),
                createNumberField("playerId", "1")
        );
        playerIdBox.setDisable(true);

        HBox gameIdsBox = new HBox(10);
        TextField gameIdsField = new TextField();
        gameIdsField.setPromptText("1,2,3,4");
        gameIdsBox.getChildren().addAll(
                new Label("Game IDs:"),
                gameIdsField
        );
        gameIdsBox.setDisable(true);

        // File selection
        HBox fileBox = new HBox(10);
        TextField fileField = new TextField();
        fileField.setPromptText("output.pgn");
        fileField.setPrefWidth(200);
        Button browseButton = new Button("Browse...");
        fileBox.getChildren().addAll(new Label("Output file:"), fileField, browseButton);

        // Progress indicator
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setVisible(false);

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: green;");

        // Buttons
        HBox buttonBox = new HBox(10);
        Button exportButton = new Button("Export");
        Button cancelButton = new Button("Cancel");
        buttonBox.getChildren().addAll(exportButton, cancelButton);

        // Event handlers
        singleGameOption.setOnAction(e -> {
            gameIdBox.setDisable(false);
            playerIdBox.setDisable(true);
            gameIdsBox.setDisable(true);
        });

        playerGamesOption.setOnAction(e -> {
            gameIdBox.setDisable(true);
            playerIdBox.setDisable(false);
            gameIdsBox.setDisable(true);
        });

        multipleGamesOption.setOnAction(e -> {
            gameIdBox.setDisable(true);
            playerIdBox.setDisable(true);
            gameIdsBox.setDisable(false);
        });

        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save PGN File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PGN Files", "*.pgn")
            );
            File file = fileChooser.showSaveDialog(dialog);
            if (file != null) {
                fileField.setText(file.getAbsolutePath());
            }
        });

        exportButton.setOnAction(e -> performExport(
                dialog, exportGroup, gameIdBox, playerIdBox, gameIdsBox,
                fileField, progressBar, statusLabel
        ));

        cancelButton.setOnAction(e -> dialog.close());

        // Layout
        layout.getChildren().addAll(
                title,
                new Separator(),
                singleGameOption, gameIdBox,
                playerGamesOption, playerIdBox,
                multipleGamesOption, gameIdsBox,
                new Separator(),
                fileBox,
                progressBar,
                statusLabel,
                buttonBox
        );

        Scene scene = new Scene(layout, 400, 450);
        dialog.setScene(scene);
        dialog.show();
    }

    /**
     * Show dialog for importing PGN files
     */
    public void showImportDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Import PGN File");
        dialog.setResizable(false);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));

        // Title
        Label title = new Label("Import Chess Games from PGN");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // File selection
        HBox fileBox = new HBox(10);
        TextField fileField = new TextField();
        fileField.setPromptText("Select PGN file...");
        fileField.setPrefWidth(250);
        Button browseButton = new Button("Browse...");
        fileBox.getChildren().addAll(new Label("PGN file:"), fileField, browseButton);

        // Player ID inputs
        HBox whitePlayerBox = new HBox(10);
        whitePlayerBox.getChildren().addAll(
                new Label("White Player ID:"),
                createNumberField("whitePlayerId", "1")
        );

        HBox blackPlayerBox = new HBox(10);
        blackPlayerBox.getChildren().addAll(
                new Label("Black Player ID:"),
                createNumberField("blackPlayerId", "2")
        );

        // Validation button
        Button validateButton = new Button("Validate File");
        validateButton.setDisable(true);

        // Progress and status
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        progressBar.setVisible(false);

        Label statusLabel = new Label();

        // Buttons
        HBox buttonBox = new HBox(10);
        Button importButton = new Button("Import");
        importButton.setDisable(true);
        Button cancelButton = new Button("Cancel");
        buttonBox.getChildren().addAll(importButton, cancelButton);

        // Event handlers
        browseButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select PGN File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PGN Files", "*.pgn")
            );
            File file = fileChooser.showOpenDialog(dialog);
            if (file != null) {
                fileField.setText(file.getAbsolutePath());
                validateButton.setDisable(false);
                importButton.setDisable(false);
            }
        });

        validateButton.setOnAction(e -> validatePgnFile(fileField.getText(), statusLabel));

        importButton.setOnAction(e -> performImport(
                dialog, fileField, whitePlayerBox, blackPlayerBox,
                progressBar, statusLabel
        ));

        cancelButton.setOnAction(e -> dialog.close());

        // Layout
        layout.getChildren().addAll(
                title,
                new Separator(),
                fileBox,
                validateButton,
                new Separator(),
                whitePlayerBox,
                blackPlayerBox,
                new Separator(),
                progressBar,
                statusLabel,
                buttonBox
        );

        Scene scene = new Scene(layout, 450, 400);
        dialog.setScene(scene);
        dialog.show();
    }

    // Helper methods
    private TextField createNumberField(String id, String defaultValue) {
        TextField field = new TextField(defaultValue);
        field.setId(id);
        field.setPrefWidth(80);

        // Only allow numbers
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        return field;
    }

    private void performExport(Stage dialog, ToggleGroup exportGroup,
                               HBox gameIdBox, HBox playerIdBox, HBox gameIdsBox,
                               TextField fileField, ProgressBar progressBar, Label statusLabel) {

        String outputFile = fileField.getText();
        if (outputFile.isEmpty()) {
            statusLabel.setText("Please specify output file");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // Show progress
        progressBar.setVisible(true);
        progressBar.setProgress(-1); // Indeterminate
        statusLabel.setText("Exporting...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    String pgnContent = "";

                    RadioButton selected = (RadioButton) exportGroup.getSelectedToggle();
                    if (selected.getText().contains("single game")) {
                        TextField gameIdField = (TextField) gameIdBox.getChildren().get(1);
                        int gameId = Integer.parseInt(gameIdField.getText());
                        pgnContent = pgnService.exportGameToPgn(gameId);

                    } else if (selected.getText().contains("player")) {
                        TextField playerIdField = (TextField) playerIdBox.getChildren().get(1);
                        int playerId = Integer.parseInt(playerIdField.getText());
                        pgnContent = pgnService.exportPlayerGamesToPgn(playerId);

                    } else if (selected.getText().contains("multiple")) {
                        TextField gameIdsField = (TextField) gameIdsBox.getChildren().get(1);
                        String[] ids = gameIdsField.getText().split(",");
                        List<Integer> gameIds = List.of(ids).stream()
                                .map(String::trim)
                                .map(Integer::parseInt)
                                .toList();
                        pgnContent = pgnService.exportGamesToPgn(gameIds);
                    }

                    pgnService.savePgnToFile(pgnContent, outputFile);

                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        statusLabel.setText("Export completed successfully!");
                        statusLabel.setStyle("-fx-text-fill: green;");
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        progressBar.setVisible(false);
                        statusLabel.setText("Export failed: " + e.getMessage());
                        statusLabel.setStyle("-fx-text-fill: red;");
                    });
                }
                return null;
            }
        };

        new Thread(exportTask).start();
    }

    private void performImport(Stage dialog, TextField fileField,
                               HBox whitePlayerBox, HBox blackPlayerBox,
                               ProgressBar progressBar, Label statusLabel) {

        String filePath = fileField.getText();
        if (filePath.isEmpty()) {
            statusLabel.setText("Please select a PGN file");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        TextField whitePlayerField = (TextField) whitePlayerBox.getChildren().get(1);
        TextField blackPlayerField = (TextField) blackPlayerBox.getChildren().get(1);

        int whitePlayerId = Integer.parseInt(whitePlayerField.getText());
        int blackPlayerId = Integer.parseInt(blackPlayerField.getText());

        // Show progress
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        statusLabel.setText("Importing...");
        statusLabel.setStyle("-fx-text-fill: blue;");

        Task<List<Game>> importTask = new Task<List<Game>>() {
            @Override
            protected List<Game> call() throws Exception {
                return pgnService.importPgnToDatabase(filePath, whitePlayerId, blackPlayerId);
            }
        };

        importTask.setOnSucceeded(e -> {
            List<Game> importedGames = importTask.getValue();
            progressBar.setVisible(false);
            statusLabel.setText("Successfully imported " + importedGames.size() + " games!");
            statusLabel.setStyle("-fx-text-fill: green;");
        });

        importTask.setOnFailed(e -> {
            progressBar.setVisible(false);
            Throwable exception = importTask.getException();
            statusLabel.setText("Import failed: " + exception.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        });

        new Thread(importTask).start();
    }

    private void validatePgnFile(String filePath, Label statusLabel) {
        if (filePath.isEmpty()) {
            statusLabel.setText("Please select a file first");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        Task<Integer> validateTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                List<PgnService.ParsedPgnGame> games = pgnService.loadPgnFromFile(filePath);
                return games.size();
            }
        };

        validateTask.setOnSucceeded(e -> {
            int gameCount = validateTask.getValue();
            statusLabel.setText("✓ Valid PGN file with " + gameCount + " games");
            statusLabel.setStyle("-fx-text-fill: green;");
        });

        validateTask.setOnFailed(e -> {
            Throwable exception = validateTask.getException();
            statusLabel.setText("✗ Invalid PGN file: " + exception.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        });

        new Thread(validateTask).start();
    }
}