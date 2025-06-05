package io.gchape.github.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ServerModel {
    private final IntegerProperty clientCount;
    private final StringProperty serverUpdates;

    public ServerModel() {
        this.serverUpdates = new SimpleStringProperty();
        this.clientCount = new SimpleIntegerProperty(0);
    }

    public StringProperty serverUpdatesProperty() {
        return serverUpdates;
    }

    public IntegerProperty clientCountProperty() {
        return clientCount;
    }
}
