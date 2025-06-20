package io.gchape.github.model.repository;

import io.gchape.github.model.entity.db.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AdminRepository {
    private static final Logger logger = LoggerFactory.getLogger(AdminRepository.class);

    private final DataSource dataSource;

    @Autowired
    public AdminRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Admin> findAll() {
        var admins = new ArrayList<Admin>();

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM admins")) {
            var rs = stmt.executeQuery();

            while (rs.next()) {
                var id = rs.getInt("id");
                var username = rs.getString("username");
                var password = rs.getString("password");

                admins.add(new Admin(id, username, password));
            }
        } catch (SQLException e) {
            logger.error("Failed to fetch all admins", e);
            throw new DataAccessException("Failed to fetch all admins", e) {
            };
        }

        return admins;
    }

    public Optional<Admin> findById(final int id) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM admins WHERE id = ?")) {
            stmt.setInt(1, id);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                var username = rs.getString("username");
                var password = rs.getString("password");
                return Optional.of(new Admin(id, username, password));
            }

            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to fetch admin by id: {}", id, e);
            throw new DataAccessException("Failed to fetch admin by id", e) {
            };
        }
    }

    public Optional<Admin> findByUsername(final String username) {
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM admins WHERE username = ?")) {
            stmt.setString(1, username);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                var id = rs.getInt("id");
                var password = rs.getString("password");
                return Optional.of(new Admin(id, username, password));
            }

            return Optional.empty();
        } catch (SQLException e) {
            logger.error("Failed to fetch admin by username: {}", username, e);
            throw new DataAccessException("Failed to fetch admin by username", e) {
            };
        }
    }
}
