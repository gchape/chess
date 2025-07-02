package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Player;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PlayerRepository {
    private static final Logger logger = LoggerFactory.getLogger(PlayerRepository.class);
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Player> playerMapper = (rs, rowNum) -> new Player(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("email")
    );

    public PlayerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Player> findAll() {
        String sql = "SELECT id, username, email FROM players ORDER BY username";
        try {
            List<Player> players = jdbcTemplate.query(sql, playerMapper);
            logger.debug("Retrieved {} players from database", players.size());
            return players;
        } catch (DataAccessException e) {
            logger.error("Error fetching all players", e);
            throw e;
        }
    }

    // Add to PlayerRepository.java


    public Optional<Player> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            logger.warn("Attempted to fetch player with null or empty email");
            return Optional.empty();
        }

        String sql = "SELECT id, username, email FROM players WHERE email = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, playerMapper, email.trim().toLowerCase()));
        } catch (DataAccessException e) {
            logger.debug("No player found with email: {}", email);
            return Optional.empty();
        }
    }

    public Optional<Player> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Attempted to fetch player with null or empty username");
            return Optional.empty();
        }

        String sql = "SELECT id, username, email FROM players WHERE username = ?";
        try {
            Player player = jdbcTemplate.queryForObject(sql, playerMapper, username.trim());
            return Optional.ofNullable(player);
        } catch (DataAccessException e) {
            logger.debug("No player found with username: {}", username);
            return Optional.empty();
        }
    }

    public Optional<Player> findById(int id) {
        if (id <= 0) {
            logger.warn("Attempted to fetch player with invalid ID: {}", id);
            return Optional.empty();
        }

        String sql = "SELECT id, username, email FROM players WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, playerMapper, id));
        } catch (DataAccessException e) {
            logger.debug("No player found with ID: {}", id);
            return Optional.empty();
        }
    }

    public boolean save(String username, String email, String password) {
        if (isInvalid(username, email, password)) {
            return false;
        }

        username = username.trim();
        email = email.trim().toLowerCase();

        if (findByUsername(username).isPresent() || findByEmail(email).isPresent()) {
            logger.warn("Attempted to create player with existing username or email");
            return false;
        }

        String sql = "INSERT INTO players (username, email, password) VALUES (?, ?, ?)";
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try {
            int rows = jdbcTemplate.update(sql, username, email, hashedPassword);
            if (rows > 0) {
                logger.info("Successfully created player: {}", username);
                return true;
            } else {
                logger.warn("Failed to create player: {}", username);
                return false;
            }
        } catch (DataAccessException e) {
            logger.error("Error inserting player: {}", username, e);
            return false;
        }
    }

    public boolean validateCredentials(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            logger.warn("Invalid credentials provided");
            return false;
        }

        Optional<String> hashed = findPasswordByUsername(username.trim());
        boolean isValid = hashed.filter(h -> BCrypt.checkpw(password, h)).isPresent();

        logger.debug("Credential validation for user {}: {}", username, isValid ? "successful" : "failed");
        return isValid;
    }

    private Optional<String> findPasswordByUsername(String username) {
        String sql = "SELECT password FROM players WHERE username = ?";
        try {
            String hash = jdbcTemplate.queryForObject(sql, String.class, username);
            return Optional.ofNullable(hash);
        } catch (DataAccessException e) {
            logger.debug("No password found for user: {}", username);
            return Optional.empty();
        }
    }

    private boolean isInvalid(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            logger.warn("Invalid username: null or empty");
            return true;
        }
        if (email == null || email.trim().isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            logger.warn("Invalid email: {}", email);
            return true;
        }
        if (password == null || password.length() < 6) {
            logger.warn("Invalid password: too short");
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
}
