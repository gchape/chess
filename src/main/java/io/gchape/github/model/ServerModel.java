package io.gchape.github.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ServerModel {
    private final StringProperty serverStatus;
    private final IntegerProperty connectedClients;

    public ServerModel() {
        this.serverStatus = new SimpleStringProperty();
        this.connectedClients = new SimpleIntegerProperty();
    }

    public StringProperty serverStatusProperty() {
        return serverStatus;
    }

    public IntegerProperty connectedClientsProperty() {
        return connectedClients;
    }
}
