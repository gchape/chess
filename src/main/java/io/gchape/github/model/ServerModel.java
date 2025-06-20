package io.gchape.github.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ServerModel {
    private final StringProperty respMessage;
    private final StringProperty serverStatus;
    private final IntegerProperty clientCount;

    private final StringProperty username;
    private final StringProperty password;

    public ServerModel() {
        username = new SimpleStringProperty("");
        password = new SimpleStringProperty("");

        clientCount = new SimpleIntegerProperty(0);
        respMessage = new SimpleStringProperty("");
        serverStatus = new SimpleStringProperty("");
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
}
