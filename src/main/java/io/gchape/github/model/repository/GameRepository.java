package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Game;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class GameRepository {
    private static final Logger logger = LoggerFactory.getLogger(GameRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Game> gameMapper = (rs, rowNum) -> {
        Timestamp startTime = rs.getTimestamp("start_time");
        return new Game(
                rs.getInt("id"),
                rs.getInt("player_white_id"),
                rs.getInt("player_black_id"),
                startTime != null ? startTime.toInstant() : null,
                rs.getString("gameplay")
        );
    };

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
            logger.error("Error fetching game by ID: {}", id, e);
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
            logger.error("Error fetching all games", e);
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
            logger.error("Error fetching games by player ID: {}", playerId, e);
            return List.of();
        }
    }

    public Optional<Game> save(Game game) {
        if (game == null) {
            throw new IllegalArgumentException("Game cannot be null");
        }

        String sql = """
                    INSERT INTO games (player_white_id, player_black_id, start_time, gameplay)
                    VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            int rows = jdbcTemplate.update(connection -> {
                // Specify only the "id" column to be returned
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setInt(1, game.playerWhiteId());
                ps.setInt(2, game.playerBlackId());
                ps.setTimestamp(3, Timestamp.from(game.startTime()));
                ps.setString(4, game.gameplay());
                return ps;
            }, keyHolder);

            if (rows == 0) {
                logger.warn("No rows inserted for game");
                return Optional.empty();
            }

            // Try multiple approaches to get the generated ID
            Integer generatedId = extractGeneratedId(keyHolder);
            if (generatedId == null) {
                logger.warn("No generated key returned for inserted game");
                return Optional.empty();
            }

            Game savedGame = new Game(
                    generatedId,
                    game.playerWhiteId(),
                    game.playerBlackId(),
                    game.startTime(),
                    game.gameplay()
            );

            logger.info("Successfully saved game with ID: {}", generatedId);
            return Optional.of(savedGame);

        } catch (DataAccessException e) {
            logger.error("Error saving game for players {} vs {}",
                    game.playerWhiteId(), game.playerBlackId(), e);
            return Optional.empty();
        }
    }

    /**
     * Robust method to extract generated ID from KeyHolder
     */
    private Integer extractGeneratedId(KeyHolder keyHolder) {
        try {
            // Method 1: Try getKeyAs first
            Number key = keyHolder.getKeyAs(Number.class);
            if (key != null) {
                return key.intValue();
            }
        } catch (Exception e) {
            logger.debug("getKeyAs failed, trying alternative methods", e);
        }

        try {
            // Method 2: Try getting from the keys map
            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                Object idValue = keys.get("id");
                if (idValue instanceof Number) {
                    return ((Number) idValue).intValue();
                }
            }
        } catch (Exception e) {
            logger.debug("getKeys failed, trying getKey method", e);
        }

        try {
            // Method 3: Try the original getKey method as last resort
            Number key = keyHolder.getKey();
            if (key != null) {
                return key.intValue();
            }
        } catch (Exception e) {
            logger.debug("getKey failed", e);
        }

        return null;
    }

    public boolean updateGameplay(int gameId, String newGameplay) {
        try {
            int rows = jdbcTemplate.update(
                    "UPDATE games SET gameplay = ? WHERE id = ?",
                    newGameplay,
                    gameId
            );
            logger.info("Updated gameplay for game ID: {}", gameId);
            return rows > 0;
        } catch (DataAccessException e) {
            logger.error("Error updating gameplay for game ID: {}", gameId, e);
            return false;
        }
    }

    public boolean deleteById(int gameId) {
        try {
            int rows = jdbcTemplate.update(
                    "DELETE FROM games WHERE id = ?",
                    gameId
            );
            logger.info("Deleted game with ID: {}", gameId);
            return rows > 0;
        } catch (DataAccessException e) {
            logger.error("Error deleting game with ID: {}", gameId, e);
            return false;
        }
    }
}