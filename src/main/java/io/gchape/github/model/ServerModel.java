package io.gchape.github.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.springframework.stereotype.Component;

@Component
public class ServerModel {
    private final StringProperty respMessage;
    private final StringProperty serverStatus;
    private final IntegerProperty clientCount;

    private final StringProperty username;
    private final StringProperty password;

    private final ObjectProperty<ObservableList<String>> databaseTableColumns;
    private final ObjectProperty<ObservableList<String>> databaseTableList;
    private final ObjectProperty<ObservableList<ObservableList<String>>> databaseTableRows;

    private final StringProperty selectedTable;

    public ServerModel() {
        username = new SimpleStringProperty("");
        password = new SimpleStringProperty("");

        clientCount = new SimpleIntegerProperty(0);
        respMessage = new SimpleStringProperty("");
        serverStatus = new SimpleStringProperty("");

        databaseTableList = new SimpleObjectProperty<>(FXCollections.emptyObservableList());
        databaseTableColumns = new SimpleObjectProperty<>(FXCollections.emptyObservableList());
        databaseTableRows = new SimpleObjectProperty<>(FXCollections.emptyObservableList());

        selectedTable = new SimpleStringProperty("");
    }

    public StringProperty respMessageProperty() {
        return respMessage;
    }

    public StringProperty serverStatusProperty() {
        return serverStatus;
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

    public ObjectProperty<ObservableList<String>> databaseTableListProperty() {
        return databaseTableList;
    }

    public ObjectProperty<ObservableList<String>> databaseTableColumnsProperty() {
        return databaseTableColumns;
    }

    public ObjectProperty<ObservableList<ObservableList<String>>> databaseTableRowsProperty() {
        return databaseTableRows;
    }

    public StringProperty selectedTableProperty() {
        return selectedTable;
    }

    // Convenience methods
    public void setDatabaseTables(ObservableList<String> tables) {
        databaseTableList.set(tables);
    }

    public void setTableColumns(ObservableList<String> columns) {
        databaseTableColumns.set(columns);
    }

    public void setTableRows(ObservableList<ObservableList<String>> rows) {
        databaseTableRows.set(rows);
    }

    public void clearTableData() {
        databaseTableColumns.set(FXCollections.emptyObservableList());
        databaseTableRows.set(FXCollections.emptyObservableList());
    }
}