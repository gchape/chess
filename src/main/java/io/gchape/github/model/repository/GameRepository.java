package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Game;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class GameRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Game> gameMapper = (rs, rowNum) -> new Game(
            rs.getInt("id"),
            rs.getInt("player_white_id"),
            rs.getInt("player_black_id"),
            rs.getTimestamp("start_time").toInstant(),
            rs.getString("gameplay")
    );

    public GameRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Game> findById(int id) {
        try {
            Game game = jdbcTemplate.queryForObject(
                    "SELECT * FROM games WHERE id = ?",
                    gameMapper,
                    id
            );
            return Optional.ofNullable(game);
        } catch (DataAccessException e) {
            System.err.println("Error fetching game by ID: " + e.getMessage());
            return Optional.empty();
        }
    }

    public List<Game> findAll() {
        try {
            return jdbcTemplate.query(
                    "SELECT * FROM games ORDER BY start_time DESC",
                    gameMapper
            );
        } catch (DataAccessException e) {
            System.err.println("Error fetching all games: " + e.getMessage());
            return List.of();
        }
    }

    public List<Game> findByPlayerId(int playerId) {
        try {
            return jdbcTemplate.query(
                    """
                            SELECT * FROM games
                            WHERE player_white_id = ? OR player_black_id = ?
                            ORDER BY start_time DESC
                            """,
                    gameMapper,
                    playerId,
                    playerId
            );
        } catch (DataAccessException e) {
            System.err.println("Error fetching games by player ID: " + e.getMessage());
            return List.of();
        }
    }

    public Optional<Game> save(Game game) {
        String sql = """
                    INSERT INTO games (player_white_id, player_black_id, start_time, gameplay)
                    VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        int rows = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, game.playerWhiteId());
            ps.setInt(2, game.playerBlackId());
            ps.setTimestamp(3, Timestamp.from(game.startTime()));
            ps.setString(4, game.gameplay());
            return ps;
        }, keyHolder);

        if (rows == 0 || keyHolder.getKey() == null) {
            return Optional.empty();
        }

        int generatedId = keyHolder.getKey().intValue();
        return Optional.of(new Game(
                generatedId,
                game.playerWhiteId(),
                game.playerBlackId(),
                game.startTime(),
                game.gameplay()
        ));
    }

    public Optional<Boolean> updateGameplay(int gameId, String newGameplay) {
        try {
            int rows = jdbcTemplate.update(
                    "UPDATE games SET gameplay = ? WHERE id = ?",
                    newGameplay,
                    gameId
            );
            return Optional.of(rows > 0);
        } catch (DataAccessException e) {
            System.err.println("Error updating gameplay: " + e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Boolean> deleteById(int gameId) {
        try {
            int rows = jdbcTemplate.update(
                    "DELETE FROM games WHERE id = ?",
                    gameId
            );
            return Optional.of(rows > 0);
        } catch (DataAccessException e) {
            System.err.println("Error deleting game: " + e.getMessage());
            return Optional.empty();
        }
    }
}
