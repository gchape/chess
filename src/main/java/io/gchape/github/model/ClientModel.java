package io.gchape.github.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.springframework.stereotype.Component;

@Component
public class ClientModel {
    private final StringProperty email;
    private final StringProperty username;
    private final StringProperty password;

    public ClientModel() {
        email = new SimpleStringProperty();
        password = new SimpleStringProperty();
        username = new SimpleStringProperty();
    }

    public StringProperty emailProperty() {
        return email;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty passwordProperty() {
        return password;
    }
}
