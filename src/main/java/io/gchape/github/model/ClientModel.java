package io.gchape.github.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.springframework.stereotype.Component;

@Component
public class ClientModel {
    private final StringProperty registerEmail;
    private final StringProperty registerUsername;
    private final StringProperty registerPassword;
    private final StringProperty loginUsername;
    private final StringProperty loginPassword;

    public ClientModel() {
        registerEmail = new SimpleStringProperty();
        registerPassword = new SimpleStringProperty();
        registerUsername = new SimpleStringProperty();
        loginUsername = new SimpleStringProperty();
        loginPassword = new SimpleStringProperty();
    }

    public StringProperty registerEmailProperty() {
        return registerEmail;
    }

    public StringProperty registerUsernameProperty() {
        return registerUsername;
    }

    public StringProperty registerPasswordProperty() {
        return registerPassword;
    }

    public StringProperty loginUsernameProperty() {
        return loginUsername;
    }

    public StringProperty loginPasswordProperty() {
        return loginPassword;
    }
}
