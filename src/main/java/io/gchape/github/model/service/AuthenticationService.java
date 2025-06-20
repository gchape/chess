package io.gchape.github.model.service;

import io.gchape.github.model.ClientModel;
import io.gchape.github.model.repository.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final ClientModel clientModel;
    private final PlayerRepository playerRepository;
    private final UIManager uiManager;

    @Autowired
    public AuthenticationService(ClientModel clientModel, PlayerRepository playerRepository, UIManager uiManager) {
        this.clientModel = clientModel;
        this.playerRepository = playerRepository;
        this.uiManager = uiManager;
    }

    public boolean handleLogin() {
        try {
            String username = getAndValidateUsername(clientModel.loginUsernameProperty().get());
            String password = getAndValidatePassword(clientModel.loginPasswordProperty().get());

            if (username == null || password == null) {
                return false;
            }

            boolean isValid = playerRepository.validateCredentials(username, password);

            if (isValid) {
                logger.info("Successful login for user: {}", username);
                clearForm();
                return true;
            } else {
                logger.warn("Failed login attempt for user: {}", username);
                uiManager.showError("Invalid username or password");
                return false;
            }

        } catch (DataAccessException e) {
            logger.error("Database error during login", e);
            uiManager.showError("Login failed due to a system error. Please try again.");
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error during login", e);
            uiManager.showError("An unexpected error occurred. Please try again.");
            return false;
        }
    }

    public boolean handleRegistration() {
        try {
            String username = getAndValidateUsername(clientModel.registerUsernameProperty().get());
            String email = getAndValidateEmail(clientModel.registerEmailProperty().get());
            String password = getAndValidatePassword(clientModel.registerPasswordProperty().get());

            if (username == null || email == null || password == null) {
                return false;
            }

            // Additional validation
            if (!isValidRegistrationData(username, email, password)) {
                return false;
            }

            boolean success = playerRepository.insertPlayer(username, email, password);

            if (success) {
                logger.info("Successful registration for user: {}", username);
                uiManager.showSuccess("Registration successful! You can now log in.");
                clearForm();
                return true;
            } else {
                logger.warn("Failed registration for user: {} with email: {}", username, email);
                uiManager.showError("Registration failed. Username or email may already exist.");
                return false;
            }

        } catch (DataAccessException e) {
            logger.error("Database error during registration", e);
            uiManager.showError("Registration failed due to a system error. Please try again.");
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            uiManager.showError("An unexpected error occurred. Please try again.");
            return false;
        }
    }

    private String getAndValidateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            uiManager.showError("Username is required");
            return null;
        }

        username = username.trim();

        if (username.length() < 3) {
            uiManager.showError("Username must be at least 3 characters long");
            return null;
        }

        if (username.length() > 50) {
            uiManager.showError("Username must be no more than 50 characters long");
            return null;
        }

        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            uiManager.showError("Username can only contain letters, numbers, underscores, and hyphens");
            return null;
        }

        return username;
    }

    private String getAndValidateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            uiManager.showError("Email is required");
            return null;
        }

        email = email.trim().toLowerCase();

        if (email.length() > 100) {
            uiManager.showError("Email must be no more than 100 characters long");
            return null;
        }

        if (!isValidEmailFormat(email)) {
            uiManager.showError("Please enter a valid email address");
            return null;
        }

        return email;
    }

    private String getAndValidatePassword(String password) {
        if (password == null || password.isEmpty()) {
            uiManager.showError("Password is required");
            return null;
        }

        if (password.length() < 6) {
            uiManager.showError("Password must be at least 6 characters long");
            return null;
        }

        if (password.length() > 100) {
            uiManager.showError("Password must be no more than 100 characters long");
            return null;
        }

        return password;
    }

    private boolean isValidRegistrationData(String username, String email, String password) {
        // Check if username already exists
        try {
            if (playerRepository.getPlayerByUsername(username).isPresent()) {
                uiManager.showError("Username already exists. Please choose a different one.");
                return false;
            }

            if (playerRepository.getPlayerByEmail(email).isPresent()) {
                uiManager.showError("Email already registered. Please use a different email or try logging in.");
                return false;
            }
        } catch (DataAccessException e) {
            logger.error("Error checking existing users during registration", e);
            uiManager.showError("Unable to verify user data. Please try again.");
            return false;
        }

        // Additional password strength validation
        if (!hasPasswordStrength(password)) {
            uiManager.showError("Password must contain at least one letter and one number");
            return false;
        }

        return true;
    }

    private boolean hasPasswordStrength(String password) {
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasLetter && hasDigit;
    }

    private boolean isValidEmailFormat(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private void clearForm() {
        try {
            clientModel.registerUsernameProperty().set("");
            clientModel.registerEmailProperty().set("");
            clientModel.registerPasswordProperty().set("");
            clientModel.loginPasswordProperty().set("");
            clientModel.loginUsernameProperty().set("");
            logger.debug("Form cleared successfully");
        } catch (Exception e) {
            logger.warn("Error clearing form", e);
        }
    }

    public void clearFormManually() {
        clearForm();
    }
}