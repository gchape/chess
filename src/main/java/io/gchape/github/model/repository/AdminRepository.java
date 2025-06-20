package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdminRepository {
    private static final Logger logger = LoggerFactory.getLogger(AdminRepository.class);
    private static final RowMapper<Admin> adminMapper = (rs, rowNum) -> new Admin(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password")
    );
    private final JdbcTemplate jdbcTemplate;

    public AdminRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Admin> findAll() {
        try {
            return jdbcTemplate.query("SELECT * FROM admins", adminMapper);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch all admins", e);
            throw e;
        }
    }

    public Optional<Admin> findById(int id) {
        try {
            var admin = jdbcTemplate.queryForObject(
                    "SELECT * FROM admins WHERE id = ?",
                    adminMapper,
                    id
            );
            return Optional.ofNullable(admin);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch admin by id: {}", id, e);
            return Optional.empty();
        }
    }

    public Optional<Admin> findByUsername(String username) {
        try {
            var admin = jdbcTemplate.queryForObject(
                    "SELECT * FROM admins WHERE username = ?",
                    adminMapper,
                    username
            );
            return Optional.ofNullable(admin);
        } catch (DataAccessException e) {
            logger.error("Failed to fetch admin by username: {}", username, e);
            return Optional.empty();
        }
    }
}
