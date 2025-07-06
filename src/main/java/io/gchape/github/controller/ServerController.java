package io.gchape.github.controller;

import io.gchape.github.controller.events.ServerOnClickEvents;
import io.gchape.github.http.server.Server;
import io.gchape.github.model.ServerModel;
import io.gchape.github.model.repository.AdminRepository;
import io.gchape.github.view.ServerView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ServerController implements ServerOnClickEvents {
    private final ServerView serverView;
    private final ServerModel serverModel;
    private final Server server;

    private final AdminRepository adminRepository;
    private final JdbcTemplate jdbcTemplate;

    private final Region mainServerViewRoot;

    @Autowired
    public ServerController(final ServerView serverView,
                            final ServerModel serverModel,
                            final AdminRepository adminRepository,
                            final JdbcTemplate jdbcTemplate,
                            final Server server) {
        this.serverView = serverView;
        this.serverModel = serverModel;
        this.adminRepository = adminRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.server = server;

        this.mainServerViewRoot = serverView.view();

        setupBindings();
    }

    private void setupBindings() {
        serverModel.serverStatusProperty()
                .bind(server.serverStatusProperty());
        serverModel.clientCountProperty()
                .bind(server.clientCountProperty());
        serverModel.respMessageProperty()
                .bind(server.respMessageProperty());

        serverModel.usernameProperty()
                .bindBidirectional(serverView.usernameProperty());
        serverModel.passwordProperty()
                .bindBidirectional(serverView.passwordProperty());

        serverView.respMessageProperty()
                .bind(serverModel.respMessageProperty());

        serverView.serverStatusProperty()
                .bind(serverModel.serverStatusProperty());

        serverView.clientCountProperty()
                .bind(serverModel.clientCountProperty());

        serverView.databaseTableListProperty()
                .bind(serverModel.databaseTableListProperty());
        serverView.databaseTableColumnsProperty()
                .bind(serverModel.databaseTableColumnsProperty());
        serverView.databaseTableRowsProperty()
                .bind(serverModel.databaseTableRowsProperty());
    }

    public void startServer(final String host, final int port) {
        server.startServer(host, port);
    }

    public void stopServer() {
        try {
            server.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onShowDatabaseClicked(final MouseEvent e) {
        var username = serverModel.usernameProperty().get();
        var password = serverModel.passwordProperty().get();

        var isValid = adminRepository.findByUsername(username)
                .map(admin -> password.equals(admin.password()))
                .orElse(false);

        if (!isValid) {
            new Alert(Alert.AlertType.ERROR, "Invalid username or password!").showAndWait();
            return;
        }

        serverModel.usernameProperty().set("");
        serverModel.passwordProperty().set("");

        loadDatabaseTables().thenRun(() ->
                Platform.runLater(() -> {
                    var scene = serverView.getScene(e);
                    scene.setRoot(serverView.databaseTableView());
                })).exceptionally(throwable -> {
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR,
                            "Failed to load database tables: " + throwable.getMessage()).showAndWait());
            return null;
        });
    }

    @Override
    public void onTableSelected(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }

        serverModel.selectedTableProperty().set(tableName);

        loadTableData(tableName).exceptionally(throwable -> {
            Platform.runLater(() ->
                    new Alert(Alert.AlertType.ERROR,
                            "Failed to load table data: " + throwable.getMessage()).showAndWait());
            return null;
        });
    }

    @Override
    public void onRefreshTableClicked(MouseEvent e) {
        String selectedTable = serverView.getSelectedTable();
        if (selectedTable != null) {
            loadDatabaseTables().thenRun(() -> {
                if (selectedTable.equals(serverView.getSelectedTable())) {
                    loadTableData(selectedTable);
                }
            });
        } else {
            loadDatabaseTables();
        }
    }

    @Override
    public void onGoBackClicked(MouseEvent e) {
        Platform.runLater(() -> {
            var scene = serverView.getScene(e);
            if (scene != null) {
                scene.setRoot(mainServerViewRoot);
            }
            serverModel.clearTableData();
            serverView.clearTableSelection();
        });
    }

    private CompletableFuture<Void> loadDatabaseTables() {
        return CompletableFuture.runAsync(() -> {
            try {
                List<String> tableNames = new ArrayList<>();

                jdbcTemplate.execute((ConnectionCallback<Object>) connection -> {
                    DatabaseMetaData metaData = connection.getMetaData();
                    try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                        while (tables.next()) {
                            tableNames.add(tables.getString("TABLE_NAME"));
                        }
                    }
                    return null;
                });

                Platform.runLater(() -> {
                    ObservableList<String> observableTableNames = FXCollections.observableArrayList(tableNames);
                    serverModel.setDatabaseTables(observableTableNames);
                });

            } catch (Exception e) {
                throw new RuntimeException("Failed to load database tables", e);
            }
        });
    }

    private CompletableFuture<Void> loadTableData(String tableName) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<String> columnNames = new ArrayList<>();
                jdbcTemplate.execute((ConnectionCallback<Object>) connection -> {
                    DatabaseMetaData metaData = connection.getMetaData();
                    try (ResultSet columns = metaData.getColumns(null, null, tableName, "%")) {
                        while (columns.next()) {
                            columnNames.add(columns.getString("COLUMN_NAME"));
                        }
                    }
                    return null;
                });

                String query = "SELECT * FROM " + tableName + " LIMIT 1000";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(query);

                ObservableList<ObservableList<String>> tableRows = FXCollections.observableArrayList();
                for (Map<String, Object> row : rows) {
                    ObservableList<String> rowData = FXCollections.observableArrayList();
                    for (String columnName : columnNames) {
                        Object value = row.get(columnName);
                        rowData.add(value != null ? value.toString() : "NULL");
                    }
                    tableRows.add(rowData);
                }

                Platform.runLater(() -> {
                    serverModel.setTableColumns(FXCollections.observableArrayList(columnNames));
                    serverModel.setTableRows(tableRows);
                });

            } catch (Exception e) {
                throw new RuntimeException("Failed to load table data for: " + tableName, e);
            }
        });
    }
}