package io.gchape.github.view;

import io.gchape.github.controller.events.ServerOnClickEvents;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.springframework.stereotype.Component;

@Component
public class ServerView {
    private final BorderPane root;
    private final Label serverStatusLabel;
    private final Label clientCountLabel;
    private final TextArea logTextArea;

    private final StringProperty username;
    private final StringProperty password;
    private final StringProperty respMessage;
    private final StringProperty serverStatus;
    private final IntegerProperty clientCount;

    private final ChoiceBox<String> databaseTableChoiceBox;
    private final ObjectProperty<ObservableList<String>> databaseTableList;
    private final ObjectProperty<ObservableList<String>> databaseTableColumns;
    private final ObjectProperty<ObservableList<ObservableList<String>>> databaseTableRows;

    private final ScrollPane databaseTableScrollPane;
    private final GridPane databaseTableGrid;
    private final Label tableInfoLabel;
    private final Button refreshTableButton;

    private final TextField usernameInput;
    private final PasswordField passwordInput;
    private final Button showDatabaseButton;

    private ServerOnClickEvents clickHandler;

    ServerView() {
        root = new BorderPane();

        username = new SimpleStringProperty("");
        password = new SimpleStringProperty("");
        serverStatus = new SimpleStringProperty("");
        respMessage = new SimpleStringProperty("");
        clientCount = new SimpleIntegerProperty(0);

        serverStatusLabel = new Label();
        clientCountLabel = new Label();
        logTextArea = new TextArea();

        databaseTableChoiceBox = new ChoiceBox<>();
        databaseTableList = new SimpleObjectProperty<>(FXCollections.emptyObservableList());
        databaseTableColumns = new SimpleObjectProperty<>(FXCollections.emptyObservableList());
        databaseTableRows = new SimpleObjectProperty<>(FXCollections.emptyObservableList());
        databaseTableGrid = new GridPane();
        databaseTableScrollPane = new ScrollPane(databaseTableGrid);
        tableInfoLabel = new Label("Select a table to view data");
        refreshTableButton = new Button("Refresh");

        usernameInput = new TextField();
        passwordInput = new PasswordField();
        showDatabaseButton = new Button("Show Database");

        setupLayout();
        setupBindings();
    }

    private void setupLayout() {
        setupTopSection();
        setupCenterSection();
        setupRightSection();
    }

    private void setupTopSection() {
        VBox statusBox = createStatusBox();
        root.setTop(statusBox);
    }

    private VBox createStatusBox() {
        VBox statusBox = new VBox();
        statusBox.getStyleClass().add("status-container");

        serverStatusLabel.getStyleClass().add("label-title");
        clientCountLabel.getStyleClass().add("label-title");

        statusBox.getChildren().addAll(serverStatusLabel, clientCountLabel);
        return statusBox;
    }

    private void setupCenterSection() {
        configureLogTextArea();
        root.setCenter(logTextArea);
        BorderPane.setMargin(logTextArea, new Insets(0, 8, 8, 8));
    }

    private void configureLogTextArea() {
        logTextArea.setWrapText(true);
        logTextArea.setEditable(false);
        logTextArea.getStyleClass().add("log-area");
    }

    private void setupRightSection() {
        var authPane = createAuthPane();
        root.setRight(authPane);
        BorderPane.setMargin(authPane, new Insets(0, 8, 8, 8));
    }

    private VBox createAuthPane() {
        VBox authPane = new VBox();
        authPane.getStyleClass().add("auth-pane");

        Label usernameLabel = new Label("Username:");
        Label passwordLabel = new Label("Password:");

        usernameLabel.getStyleClass().add("form-label");
        passwordLabel.getStyleClass().add("form-label");

        usernameInput.setPromptText("e.g. admin");
        usernameInput.getStyleClass().add("text-field");

        passwordInput.setPromptText("enter password");
        passwordInput.getStyleClass().add("password-field");

        authPane.getChildren().addAll(
                usernameLabel,
                usernameInput,
                passwordLabel,
                passwordInput,
                showDatabaseButton
        );
        return authPane;
    }

    private void setupBindings() {
        bindServerStatus();
        bindClientCount();
        bindRespMessage();
        bindFormInputs();
        bindDatabaseProperties();
    }

    private void bindServerStatus() {
        serverStatus.addListener((obs, old, current) ->
                Platform.runLater(() -> serverStatusLabel.setText(current)));
    }

    private void bindClientCount() {
        clientCount.addListener((obs, old, current) ->
                Platform.runLater(() ->
                        clientCountLabel.setText("Connected Clients: %d".formatted(current.intValue()))));
    }

    private void bindRespMessage() {
        respMessage.addListener((obs, old, current) ->
                Platform.runLater(() -> logTextArea.appendText(current)));
    }

    private void bindFormInputs() {
        username.bindBidirectional(usernameInput.textProperty());
        password.bindBidirectional(passwordInput.textProperty());
    }

    private void bindDatabaseProperties() {
        databaseTableList.addListener((obs, old, current) ->
                Platform.runLater(() -> {
                    databaseTableChoiceBox.setItems(current);
                    if (current != null && !current.isEmpty()) {
                        databaseTableChoiceBox.setValue(current.getFirst());
                    }
                }));

        databaseTableChoiceBox.setOnAction(e -> {
            if (clickHandler != null) {
                String selectedTable = databaseTableChoiceBox.getValue();
                if (selectedTable != null) {
                    clickHandler.onTableSelected(selectedTable);
                }
            }
        });

        databaseTableColumns.addListener((obs, old, current) ->
                Platform.runLater(this::updateTableView));

        databaseTableRows.addListener((obs, old, current) ->
                Platform.runLater(this::updateTableView));
    }

    private void updateTableView() {
        databaseTableGrid.getChildren().clear();
        databaseTableGrid.getRowConstraints().clear();
        databaseTableGrid.getColumnConstraints().clear();

        ObservableList<String> columns = databaseTableColumns.get();
        ObservableList<ObservableList<String>> rows = databaseTableRows.get();

        if (columns == null || columns.isEmpty()) {
            tableInfoLabel.setText("No columns available");
            return;
        }

        databaseTableGrid.setHgap(1);
        databaseTableGrid.setVgap(1);
        databaseTableGrid.setPadding(new Insets(10));

        for (int col = 0; col < columns.size(); col++) {
            Label header = new Label(columns.get(col));
            header.getStyleClass().addAll("db-column-header", "db-cell");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);

            ColumnConstraints columnConstraints = new ColumnConstraints();
            columnConstraints.setHgrow(Priority.ALWAYS);
            columnConstraints.setMinWidth(100);
            columnConstraints.setPrefWidth(150);
            databaseTableGrid.getColumnConstraints().add(columnConstraints);

            databaseTableGrid.add(header, col, 0);
        }

        if (rows != null && !rows.isEmpty()) {
            for (int row = 0; row < rows.size(); row++) {
                ObservableList<String> rowData = rows.get(row);
                for (int col = 0; col < Math.min(columns.size(), rowData.size()); col++) {
                    Label cell = new Label(rowData.get(col) != null ? rowData.get(col) : "NULL");
                    cell.getStyleClass().addAll("db-cell", "db-data-cell");
                    cell.setMaxWidth(Double.MAX_VALUE);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    databaseTableGrid.add(cell, col, row + 1);
                }
            }
            tableInfoLabel.setText(String.format("Showing %d rows, %d columns",
                    rows.size(), columns.size()));
        } else {
            Label noDataLabel = new Label("No data available");
            noDataLabel.getStyleClass().add("no-data-label");
            noDataLabel.setAlignment(Pos.CENTER);
            databaseTableGrid.add(noDataLabel, 0, 1, columns.size(), 1);
            tableInfoLabel.setText("No data available for selected table");
        }
    }

    public Region view() {
        return root;
    }

    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public StringProperty respMessageProperty() {
        return respMessage;
    }

    public IntegerProperty clientCountProperty() {
        return clientCount;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public void setOnClickHandler(final ServerOnClickEvents onClickHandler) {
        this.clickHandler = onClickHandler;
        showDatabaseButton.setOnMouseClicked(onClickHandler::onShowDatabaseClicked);
        refreshTableButton.setOnMouseClicked(onClickHandler::onRefreshTableClicked);
    }

    public Scene getScene(final MouseEvent e) {
        return ((Node) e.getSource()).getScene();
    }

    public Region databaseTableView() {
        var root = new BorderPane();
        root.getStyleClass().add("database-view");

        var topSection = createDatabaseTopSection();
        root.setTop(topSection);

        setupDatabaseTableScrollPane();
        root.setCenter(databaseTableScrollPane);

        var bottomSection = createDatabaseBottomSection();
        root.setBottom(bottomSection);

        return root;
    }

    private VBox createDatabaseTopSection() {
        VBox topSection = new VBox(10);
        topSection.setPadding(new Insets(10));
        topSection.getStyleClass().add("db-top-section");

        Label titleLabel = new Label("Database Tables");
        titleLabel.getStyleClass().add("db-title");

        HBox controlsRow = new HBox(10);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        Button goBackButton = new Button("← Back to Server");
        goBackButton.getStyleClass().add("back-button");
        goBackButton.setOnMouseClicked(e -> {
            if (clickHandler != null) {
                clickHandler.onGoBackClicked(e);
            }
        });

        Label tableLabel = new Label("Select Table:");
        tableLabel.getStyleClass().add("form-label");

        databaseTableChoiceBox.getStyleClass().add("table-choice-box");
        databaseTableChoiceBox.setMaxWidth(200);

        refreshTableButton.getStyleClass().add("refresh-button");

        controlsRow.getChildren().addAll(goBackButton, new Separator(),
                tableLabel, databaseTableChoiceBox, refreshTableButton);

        topSection.getChildren().addAll(titleLabel, controlsRow);
        return topSection;
    }

    private void setupDatabaseTableScrollPane() {
        databaseTableScrollPane.setFitToWidth(true);
        databaseTableScrollPane.setFitToHeight(true);
        databaseTableScrollPane.getStyleClass().add("table-scroll-pane");

        databaseTableGrid.getStyleClass().add("database-table-grid");
    }

    private HBox createDatabaseBottomSection() {
        HBox bottomSection = new HBox();
        bottomSection.setPadding(new Insets(10));
        bottomSection.setAlignment(Pos.CENTER_LEFT);
        bottomSection.getStyleClass().add("db-bottom-section");

        tableInfoLabel.getStyleClass().add("table-info-label");
        bottomSection.getChildren().add(tableInfoLabel);

        return bottomSection;
    }

    public ObjectProperty<ObservableList<String>> databaseTableListProperty() {
        return databaseTableList;
    }

    public ObjectProperty<ObservableList<String>> databaseTableColumnsProperty() {
        return databaseTableColumns;
    }

    public ObjectProperty<ObservableList<ObservableList<String>>> databaseTableRowsProperty() {
        return databaseTableRows;
    }

    public String getSelectedTable() {
        return databaseTableChoiceBox.getValue();
    }

    public void clearTableSelection() {
        Platform.runLater(() -> {
            databaseTableChoiceBox.setValue(null);
            databaseTableGrid.getChildren().clear();
            tableInfoLabel.setText("Select a table to view data");
        });
    }
}