package io.gchape.github.model.service;

import io.gchape.github.model.ClientModel;
import io.gchape.github.model.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private final ClientModel clientModel;
    private final PlayerRepository playerRepository;

    @Autowired
    public AuthenticationService(ClientModel clientModel, PlayerRepository playerRepository) {
        this.clientModel = clientModel;
        this.playerRepository = playerRepository;
    }

    public boolean handleLogin() {
        // TODO: Implement actual authentication logic
        String username = clientModel.usernameProperty().get();
        String password = clientModel.passwordProperty().get();

        if (validateCredentials(username, password)) {
            return true;
        }

        return false;
    }

    public boolean handleRegistration() {
        String username = clientModel.usernameProperty().get();
        String email = clientModel.emailProperty().get();
        String password = clientModel.passwordProperty().get();

        if (registerUser(username, email, password)) {
            return true;
        }

        return false;
    }

    private boolean validateCredentials(String username, String password) {
        // TODO: Implement actual validation
        return username != null && !username.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
    }

    private boolean registerUser(String username, String email, String password) {
        // TODO: Implement actual registration
        return username != null && !username.trim().isEmpty() &&
                email != null && !email.trim().isEmpty() &&
                password != null && !password.trim().isEmpty();
    }
}
