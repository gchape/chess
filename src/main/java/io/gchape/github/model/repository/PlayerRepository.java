package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Player;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PlayerRepository {
    private final DataSource datasource;

    @Autowired
    public PlayerRepository(DataSource datasource) {
        this.datasource = datasource;
    }

    @PostConstruct
    private void initializeSchema() {
        String sql = """
                    CREATE TABLE IF NOT EXISTS players (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        username VARCHAR(50) NOT NULL UNIQUE,
                        email VARCHAR(100) NOT NULL UNIQUE,
                        password VARCHAR(100) NOT NULL
                    )
                """;

        try (Connection conn = datasource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException ignored) {
        }
    }

    public List<Player> getAllPlayers() {
        List<Player> players = new ArrayList<>();
        String sql = "SELECT id, username, email FROM players";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                players.add(new Player(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching players: " + e.getMessage());
        }

        return players;
    }

    public Optional<Player> getPlayerByEmail(String email) {
        String sql = "SELECT id, username, email FROM players WHERE email = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Player(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching player by email: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Player> getPlayerByUsername(String username) {
        String sql = "SELECT id, username, email FROM players WHERE username = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Player(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching player by username: " + e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Player> getPlayerById(int id) {
        String sql = "SELECT id, username, email FROM players WHERE id = ?";

        try (Connection conn = datasource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Player(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("email")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching player by ID: " + e.getMessage());
        }

        return Optional.empty();
    }
}
