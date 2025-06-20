package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Player;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRepository.class);

    private final DataSource datasource;

    @Autowired
    public PlayerRepository(DataSource datasource) {
        this.datasource = datasource;
    }

    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT id, username, email FROM players ORDER BY username";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                players.add(createPlayerFromResultSet(rs));
            }
            logger.debug("Retrieved {} players from database", players.size());

        } catch (SQLException e) {
            logger.error("Error fetching all players", e);
            throw new DataAccessException("Failed to retrieve players", e) {
            };
        }

        return players;
    }

    public Optional<Player> getPlayerByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Attempted to fetch player with null or empty email");
            return Optional.empty();
        }

        String sql = "SELECT id, username, email FROM players WHERE email = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Player player = createPlayerFromResultSet(rs);
                    logger.debug("Found player by email: {}", email);
                    return Optional.of(player);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching player by email: {}", email, e);
            throw new DataAccessException("Failed to retrieve player by email", e) {
            };
        }

        logger.debug("No player found with email: {}", email);
        return Optional.empty();
    }

    public Optional<Player> getPlayerByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Attempted to fetch player with null or empty username");
            return Optional.empty();
        }

        String sql = "SELECT id, username, email FROM players WHERE username = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Player player = createPlayerFromResultSet(rs);
                    logger.debug("Found player by username: {}", username);
                    return Optional.of(player);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching player by username: {}", username, e);
            throw new DataAccessException("Failed to retrieve player by username", e) {
            };
        }

        logger.debug("No player found with username: {}", username);
        return Optional.empty();
    }

    public Optional<Player> getPlayerById(int id) {
        if (id <= 0) {
            logger.warn("Attempted to fetch player with invalid ID: {}", id);
            return Optional.empty();
        }

        String sql = "SELECT id, username, email FROM players WHERE id = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Player player = createPlayerFromResultSet(rs);
                    logger.debug("Found player by ID: {}", id);
                    return Optional.of(player);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching player by ID: {}", id, e);
            throw new DataAccessException("Failed to retrieve player by ID", e) {
            };
        }

        logger.debug("No player found with ID: {}", id);
        return Optional.empty();
    }

    public boolean insertPlayer(String username, String email, String password) {
        if (isValidAll(username, email, password)) {
            return false;
        }

        username = username.trim();
        email = email.trim().toLowerCase();

        if (getPlayerByUsername(username).isPresent()) {
            logger.warn("Attempted to create player with existing username: {}", username);
            return false;
        }

        if (getPlayerByEmail(email).isPresent()) {
            logger.warn("Attempted to create player with existing email: {}", email);
            return false;
        }

        String sql = "INSERT INTO players (username, email, password) VALUES (?, ?, ?)";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, hashedPassword);

            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;

            if (success) {
                logger.info("Successfully created player: {}", username);
            } else {
                logger.warn("Failed to create player: {}", username);
            }

            return success;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                logger.warn("Duplicate entry attempted for username: {} or email: {}", username, email);
            } else {
                logger.error("Error inserting player: {}", username, e);
            }
            return false;
        }
    }

    public boolean validateCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            logger.warn("Invalid credentials provided - null or empty username/password");
            return false;
        }

        Optional<String> passwordHash = getPasswordHashForUser(username.trim());

        if (passwordHash.isPresent()) {
            boolean isValid = BCrypt.checkpw(password, passwordHash.get());
            logger.debug("Credential validation for user {}: {}", username, isValid ? "successful" : "failed");
            return isValid;
        }

        logger.debug("No user found for credential validation: {}", username);
        return false;
    }

    private Optional<String> getPasswordHashForUser(String username) {
        String sql = "SELECT password FROM players WHERE username = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("password"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching password hash for user: {}", username, e);
            throw new DataAccessException("Failed to retrieve password hash", e) {
            };
        }

        return Optional.empty();
    }

    public boolean updatePassword(String username, String newPassword) {
        if (isValidAll(username, "dummy@email.com", newPassword)) {
            return false;
        }

        String sql = "UPDATE players SET password = ? WHERE username = ?";
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, hashedPassword);
            stmt.setString(2, username.trim());

            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;

            if (success) {
                logger.info("Successfully updated password for user: {}", username);
            } else {
                logger.warn("No user found to update password: {}", username);
            }

            return success;

        } catch (SQLException e) {
            logger.error("Error updating password for user: {}", username, e);
            return false;
        }
    }

    public boolean deletePlayer(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Attempted to delete player with null or empty username");
            return false;
        }

        String sql = "DELETE FROM players WHERE username = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            int affectedRows = stmt.executeUpdate();
            boolean success = affectedRows > 0;

            if (success) {
                logger.info("Successfully deleted player: {}", username);
            } else {
                logger.warn("No player found to delete: {}", username);
            }

            return success;

        } catch (SQLException e) {
            logger.error("Error deleting player: {}", username, e);
            return false;
        }
    }

    private Player createPlayerFromResultSet(ResultSet rs) throws SQLException {
        return new Player(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email")
        );
    }

    private boolean isValidAll(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Invalid username: null or empty");
            return true;
        }

        if (email == null || email.trim().isEmpty() || !isValidEmail(email)) {
            logger.warn("Invalid email: {}", email);
            return true;
        }

        if (password == null || password.length() < 6) {
            logger.warn("Invalid password: null or too short");
            return true;
        }

        if (username.trim().length() > 50) {
            logger.warn("Username too long: {}", username.length());
            return true;
        }

        if (email.trim().length() > 100) {
            logger.warn("Email too long: {}", email.length());
            return true;
        }

        return false;
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}