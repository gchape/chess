package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GameRepository {
    private final DataSource datasource;

    @Autowired
    public GameRepository(DataSource datasource) {
        this.datasource = datasource;
    }

    public Optional<Game> getGameById(int id) {
        String sql = "SELECT * FROM games WHERE id = ?";
        try (var conn = datasource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToGame(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching game by ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Game> getAllGames() {
        String sql = "SELECT * FROM games ORDER BY start_time DESC";
        List<Game> games = new ArrayList<>();
        try (var conn = datasource.getConnection();
             var stmt = conn.prepareStatement(sql);
             var rs = stmt.executeQuery()) {
            while (rs.next()) {
                games.add(mapRowToGame(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all games: " + e.getMessage());
        }
        return games;
    }

    public List<Game> getGamesByPlayerId(int playerId) {
        String sql = """
                    SELECT * FROM games
                    WHERE player_white_id = ? OR player_black_id = ?
                    ORDER BY start_time DESC
                """;
        List<Game> games = new ArrayList<>();
        try (var conn = datasource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playerId);
            stmt.setInt(2, playerId);
            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    games.add(mapRowToGame(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching games by player ID: " + e.getMessage());
        }
        return games;
    }

    public Optional<Game> persist(final Game game) {
        String sql = """
                    INSERT INTO games (player_white_id, player_black_id, start_time, gameplay)
                    VALUES (?, ?, ?, ?)
                """;

        try (var conn = datasource.getConnection();
             var stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, game.playerWhiteId());
            stmt.setInt(2, game.playerBlackId());
            stmt.setTimestamp(3, Timestamp.from(game.startTime()));
            stmt.setString(4, game.gameplay());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return Optional.empty();
            }

            try (var generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    return Optional.of(new Game(
                            generatedId,
                            game.playerWhiteId(),
                            game.playerBlackId(),
                            game.startTime(),
                            game.gameplay()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving game: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Boolean> updateGameplay(int gameId, String newGameplay) {
        String sql = "UPDATE games SET gameplay = ? WHERE id = ?";
        try (var conn = datasource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newGameplay);
            stmt.setInt(2, gameId);
            return Optional.of(stmt.executeUpdate() > 0);
        } catch (SQLException e) {
            System.err.println("Error updating gameplay: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Boolean> deleteGameById(int gameId) {
        String sql = "DELETE FROM games WHERE id = ?";
        try (var conn = datasource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, gameId);
            return Optional.of(stmt.executeUpdate() > 0);
        } catch (SQLException e) {
            System.err.println("Error deleting game: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Game mapRowToGame(ResultSet rs) throws SQLException {
        return new Game(
                rs.getInt("id"),
                rs.getInt("player_white_id"),
                rs.getInt("player_black_id"),
                rs.getTimestamp("start_time").toInstant(),
                rs.getString("gameplay")
        );
    }
}
