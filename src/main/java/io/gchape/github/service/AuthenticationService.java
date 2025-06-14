package io.gchape.github.service;

import io.gchape.github.model.message.AuthResult;

import java.sql.SQLException;
import java.util.Optional;

public class AuthenticationService {
    private DatabaseService databaseService;

    public AuthenticationService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public AuthResult authenticateUser(String username, String password) {
        try {
            Optional<Long> userId = databaseService.authenticateUser(username, password);
            if (userId.isPresent()) {
                return new AuthResult(true, "Authentication successful", userId.get());
            } else {
                return new AuthResult(false, "Invalid credentials", null);
            }
        } catch (SQLException e) {
            return new AuthResult(false, "Database error: " + e.getMessage(), null);
        }
    }

    public AuthResult registerUser(String username, String password, String email) {
        try {
            Long userId = databaseService.registerUser(username, password, email);
            return new AuthResult(true, "Registration successful", userId);
        } catch (SQLException e) {
            return new AuthResult(false, "Registration failed: " + e.getMessage(), null);
        }
    }
}


/**
 * Service class for handling user authentication, registration, and password management.
 */
//public class AuthenticationService {
//    private final DatabaseService databaseService;
//    private final SecureRandom secureRandom;
//    private static final String HASH_ALGORITHM = "SHA-256";
//    private static final int SALT_LENGTH = 16;
//
//    // Password validation patterns
//    private static final Pattern EMAIL_PATTERN = Pattern.compile(
//            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
//    );
//    private static final int MIN_PASSWORD_LENGTH = 6;
//    private static final int MIN_USERNAME_LENGTH = 3;
//    private static final int MAX_USERNAME_LENGTH = 20;
//
//    public AuthenticationService(DatabaseService databaseService) {
//        this.databaseService = databaseService;
//        this.secureRandom = new SecureRandom();
//    }
//
//    /**
//     * Registers a new user with the provided credentials.
//     *
//     * @param username The desired username
//     * @param password The plain text password
//     * @param email The user's email address
//     * @return AuthResult containing success status and user ID or error message
//     */
//    public AuthResult registerUser(String username, String password, String email) {
//        try {
//            // Validate input
//            ValidationResult validation = validateRegistrationInput(username, password, email);
//            if (!validation.isValid()) {
//                return AuthResult.failure(validation.getErrorMessage());
//            }
//
//            // Check if username already exists
//            if (databaseService.usernameExists(username)) {
//                return AuthResult.failure("Username already exists");
//            }
//
//            // Check if email already exists
//            if (databaseService.emailExists(email)) {
//                return AuthResult.failure("Email already registered");
//            }
//
//            // Register user using DatabaseService
//            Long userId = databaseService.registerUser(username, password, email);
//
//            if (userId != null) {
//                return AuthResult.success(userId, "Registration successful");
//            } else {
//                return AuthResult.failure("Registration failed due to database error");
//            }
//
//        } catch (Exception e) {
//            return AuthResult.failure("Registration failed: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Authenticates a user with username and password.
//     *
//     * @param username The username
//     * @param password The plain text password
//     * @return AuthResult containing success status and user ID or error message
//     */
//    public AuthResult authenticateUser(String username, String password) {
//        try {
//            if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
//                return AuthResult.failure("Username and password are required");
//            }
//
//            // Use DatabaseService authentication
//            Optional<Long> userIdOpt = databaseService.authenticateUser(username, password);
//            if (userIdOpt.isPresent()) {
//                Long userId = userIdOpt.get();
//
//                // Update last login
//                databaseService.updateUserLastLogin(userId);
//
//                return AuthResult.success(userId, "Authentication successful");
//            } else {
//                return AuthResult.failure("Invalid username or password");
//            }
//
//        } catch (Exception e) {
//            return AuthResult.failure("Authentication failed: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Changes a user's password.
//     *
//     * @param userId The user ID
//     * @param currentPassword The current password
//     * @param newPassword The new password
//     * @return AuthResult indicating success or failure
//     */
//    public AuthResult changePassword(Long userId, String currentPassword, String newPassword) {
//        try {
//            // Validate new password
//            if (!isValidPassword(newPassword)) {
//                return AuthResult.failure("New password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
//            }
//
//            // Get user
//            Optional<User> userOpt = databaseService.getUserById(userId);
//            if (!userOpt.isPresent()) {
//                return AuthResult.failure("User not found");
//            }
//
//            User user = userOpt.get();
//
//            // Verify current password using DatabaseService authentication
//            Optional<Long> authResult = databaseService.authenticateUser(user.getUsername(), currentPassword);
//            if (!authResult.isPresent()) {
//                return AuthResult.failure("Current password is incorrect");
//            }
//
//            // Update password
//            boolean updated = databaseService.updateUserPassword(userId, newPassword);
//
//            if (updated) {
//                return AuthResult.success(userId, "Password changed successfully");
//            } else {
//                return AuthResult.failure("Failed to update password");
//            }
//
//        } catch (Exception e) {
//            return AuthResult.failure("Password change failed: " + e.getMessage());
//        }
//    }
//
//    /**
//     * Validates registration input.
//     */
//    private ValidationResult validateRegistrationInput(String username, String password, String email) {
//        if (username == null || username.trim().isEmpty()) {
//            return ValidationResult.invalid("Username is required");
//        }
//
//        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
//            return ValidationResult.invalid("Username must be between " + MIN_USERNAME_LENGTH +
//                    " and " + MAX_USERNAME_LENGTH + " characters");
//        }
//
//        if (!username.matches("^[a-zA-Z0-9_]+$")) {
//            return ValidationResult.invalid("Username can only contain letters, numbers, and underscores");
//        }
//
//        if (!isValidPassword(password)) {
//            return ValidationResult.invalid("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
//        }
//
//        if (email == null || email.trim().isEmpty()) {
//            return ValidationResult.invalid("Email is required");
//        }
//
//        if (!EMAIL_PATTERN.matcher(email).matches()) {
//            return ValidationResult.invalid("Invalid email format");
//        }
//
//        return ValidationResult.valid();
//    }
//
//    /**
//     * Validates password strength.
//     */
//    private boolean isValidPassword(String password) {
//        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
//    }
//
//    /**
//     * Hashes a password with salt using SHA-256.
//     * This method is kept for backward compatibility but delegates to DatabaseService for actual usage.
//     */
//    private String hashPassword(String password) {
//        try {
//            // Generate salt
//            byte[] salt = new byte[SALT_LENGTH];
//            secureRandom.nextBytes(salt);
//
//            // Hash password with salt
//            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
//            md.update(salt);
//            byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));
//
//            // Combine salt and hash
//            byte[] combined = new byte[salt.length + hashedPassword.length];
//            System.arraycopy(salt, 0, combined, 0, salt.length);
//            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
//
//            return Base64.getEncoder().encodeToString(combined);
//
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException("Failed to hash password", e);
//        }
//    }
//
//    /**
//     * Verifies a password against its hash.
//     * This method is kept for backward compatibility.
//     */
//    private boolean verifyPassword(String password, String hashedPassword) {
//        try {
//            byte[] combined = Base64.getDecoder().decode(hashedPassword);
//
//            // Extract salt
//            byte[] salt = new byte[SALT_LENGTH];
//            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
//
//            // Extract hash
//            byte[] hash = new byte[combined.length - SALT_LENGTH];
//            System.arraycopy(combined, SALT_LENGTH, hash, 0, hash.length);
//
//            // Hash the provided password with the same salt
//            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
//            md.update(salt);
//            byte[] testHash = md.digest(password.getBytes(StandardCharsets.UTF_8));
//
//            // Compare hashes
//            return MessageDigest.isEqual(hash, testHash);
//
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    /**
//     * Result of authentication operations.
//     */
//    public static class AuthResult {
//        private final boolean success;
//        private final Long userId;
//        private final String message;
//
//        private AuthResult(boolean success, Long userId, String message) {
//            this.success = success;
//            this.userId = userId;
//            this.message = message;
//        }
//
//        public static AuthResult success(Long userId, String message) {
//            return new AuthResult(true, userId, message);
//        }
//
//        public static AuthResult failure(String message) {
//            return new AuthResult(false, null, message);
//        }
//
//        public boolean isSuccess() {
//            return success;
//        }
//
//        public Long getUserId() {
//            return userId;
//        }
//
//        public String getMessage() {
//            return message;
//        }
//    }
//
//    /**
//     * Result of input validation.
//     */
//    private static class ValidationResult {
//        private final boolean valid;
//        private final String errorMessage;
//
//        private ValidationResult(boolean valid, String errorMessage) {
//            this.valid = valid;
//            this.errorMessage = errorMessage;
//        }
//
//        public static ValidationResult valid() {
//            return new ValidationResult(true, null);
//        }
//
//        public static ValidationResult invalid(String errorMessage) {
//            return new ValidationResult(false, errorMessage);
//        }
//
//        public boolean isValid() {
//            return valid;
//        }
//
//        public String getErrorMessage() {
//            return errorMessage;
//        }
//    }
//}