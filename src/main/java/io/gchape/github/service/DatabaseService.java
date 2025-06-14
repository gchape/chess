package io.gchape.github.service;

import io.gchape.github.model.entity.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

// Database Service for Chess Game Recording
public class DatabaseService {
    // Modified connection string with better lock handling and unique database per instance
    private static final String DB_URL = "jdbc:h2:mem:chess_db;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private Connection connection;
    private static volatile DatabaseService instance;
    private static final Object lock = new Object();

    // Private constructor - this is key for singleton pattern
    private DatabaseService() {
        initializeDatabase();
    }

    // Singleton pattern to ensure only one instance
    public static DatabaseService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DatabaseService();
                }
            }
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            // Force close any existing connections and clean up lock files
            cleanupExistingConnections();

            Class.forName("org.h2.Driver");

            // Try to create connection with retries
            int retries = 3;
            SQLException lastException = null;

            for (int i = 0; i < retries; i++) {
                try {
                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                    connection.setAutoCommit(true);
                    break;
                } catch (SQLException e) {
                    lastException = e;
                    System.err.println("Connection attempt " + (i + 1) + " failed: " + e.getMessage());

                    if (i < retries - 1) {
                        // Wait and try cleanup again
                        Thread.sleep(2000);
                        cleanupExistingConnections();
                    }
                }
            }

            if (connection == null) {
                throw new RuntimeException("Failed to create database connection after " + retries + " attempts", lastException);
            }

            createTables();

            // Add shutdown hook for proper cleanup
            addShutdownHook();

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void cleanupExistingConnections() {
        try {
            // Try to close any existing connections to the same database
            try (Connection shutdownConn = DriverManager.getConnection(
                    "jdbc:h2:./chess_db;SHUTDOWN=TRUE", DB_USER, DB_PASSWORD)) {
                // This will close the database if it's open
            } catch (SQLException e) {
                // Ignore - database might not be open
            }

            // Clean up lock files if they exist
            java.io.File lockFile = new java.io.File("chess_db.lock.db");
            if (lockFile.exists()) {
                lockFile.delete();
            }

            // Small delay to ensure cleanup is complete
            Thread.sleep(1000);

        } catch (Exception e) {
            System.err.println("Warning: Could not clean up existing connections: " + e.getMessage());
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
                // Force shutdown of H2 database
                try (Connection shutdownConn = DriverManager.getConnection(
                        "jdbc:h2:./chess_db;SHUTDOWN=TRUE", DB_USER, DB_PASSWORD)) {
                    // This will properly close the database
                }
            } catch (SQLException e) {
                // Ignore during shutdown
            }
        }));
    }

    // Rest of your methods remain the same...
    private void createTables() throws SQLException {
        String[] createTableQueries = {
                """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS games (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                white_player_id BIGINT,
                black_player_id BIGINT,
                game_mode VARCHAR(20) NOT NULL,
                game_result VARCHAR(20) DEFAULT 'ONGOING',
                start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                end_time TIMESTAMP,
                total_moves INT DEFAULT 0,
                pgn_notation TEXT,
                FOREIGN KEY (white_player_id) REFERENCES users(id),
                FOREIGN KEY (black_player_id) REFERENCES users(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS moves (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                game_id BIGINT NOT NULL,
                move_number INT NOT NULL,
                player_color VARCHAR(5) NOT NULL,
                from_position VARCHAR(2) NOT NULL,
                to_position VARCHAR(2) NOT NULL,
                piece_type VARCHAR(10) NOT NULL,
                captured_piece VARCHAR(10),
                special_move VARCHAR(20),
                promotion_piece VARCHAR(10),
                is_check BOOLEAN DEFAULT FALSE,
                is_checkmate BOOLEAN DEFAULT FALSE,
                move_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                algebraic_notation VARCHAR(10),
                FOREIGN KEY (game_id) REFERENCES games(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS game_spectators (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                game_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (game_id) REFERENCES games(id),
                FOREIGN KEY (user_id) REFERENCES users(id)
            )
            """,
                """
            CREATE TABLE IF NOT EXISTS pgn_files (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                file_path VARCHAR(500) NOT NULL,
                content TEXT NOT NULL,
                imported_by BIGINT,
                imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (imported_by) REFERENCES users(id)
            )
            """
        };

        for (String query : createTableQueries) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(query);
            }
        }
    }


    /**
     * Check if connection is valid and reconnect if needed
     */
    public void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(5)) {
            System.out.println("Reconnecting to database...");
            initializeDatabase();
        }
    }


    /**
     * Register a new user with hashed password
     */
    public Long registerUser(String username, String password, String email) throws SQLException {
        ensureConnection();
        String query = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
            stmt.setString(3, email);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Failed to get generated user ID");
        }
    }

    /**
     * Authenticate user and return user ID if successful
     */
    public Optional<Long> authenticateUser(String username, String password) throws SQLException {
        ensureConnection();
        String query = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(rs.getLong("id"));
            }
            return Optional.empty();
        }
    }

    /**
     * Check if username exists
     */
    public boolean usernameExists(String username) throws SQLException {
        ensureConnection();
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) throws SQLException {
        ensureConnection();
        String query = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    /**
     * Get user by username
     */
    public Optional<User> getUserByUsername(String username) throws SQLException {
        ensureConnection();
        String query = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email")
                );
                user.setId(rs.getLong("id"));
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    user.setLastLogin(lastLogin.toLocalDateTime());
                }
                return Optional.of(user);
            }
            return Optional.empty();
        }
    }

    /**
     * Get user by ID
     */
    public Optional<User> getUserById(Long userId) throws SQLException {
        ensureConnection();
        String query = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email")
                );
                user.setId(rs.getLong("id"));
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    user.setLastLogin(lastLogin.toLocalDateTime());
                }
                return Optional.of(user);
            }
            return Optional.empty();
        }
    }

    /**
     * Update user's last login time
     */
    public void updateUserLastLogin(Long userId) throws SQLException {
        ensureConnection();
        String query = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Update user password
     */
    public boolean updateUserPassword(Long userId, String newPassword) throws SQLException {
        ensureConnection();
        String query = "UPDATE users SET password = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, hashPassword(newPassword));
            stmt.setLong(2, userId);
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        }
    }


    public Long createGame(Long whitePlayerId, Long blackPlayerId, String gameMode) throws SQLException {
        ensureConnection();
        String query = "INSERT INTO games (white_player_id, black_player_id, game_mode) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, whitePlayerId);
            if (blackPlayerId != null) {
                stmt.setLong(2, blackPlayerId);
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, gameMode);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Failed to get generated game ID");
        }
    }

    /**
     * Update game to add a player (for joining existing games)
     */
    public void updateGamePlayer(Long gameId, Long playerId, String color) throws SQLException {
        ensureConnection();
        String query;
        if ("WHITE".equals(color)) {
            query = "UPDATE games SET white_player_id = ? WHERE id = ?";
        } else {
            query = "UPDATE games SET black_player_id = ? WHERE id = ?";
        }

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, playerId);
            stmt.setLong(2, gameId);
            stmt.executeUpdate();
        }
    }

    /**
     * Record a move - overloaded method for compatibility with GameSessionManager
     */
    public void recordMove(Long gameId, int moveNumber, String fromPos, String toPos,
                           String pieceType, String capturedPiece, String specialMove,
                           boolean isCheck, boolean isCheckmate, String algebraicNotation) throws SQLException {
        // Determine player color based on move number
        String playerColor = (moveNumber % 2 == 1) ? "WHITE" : "BLACK";

        recordMove(gameId, moveNumber, playerColor, fromPos, toPos, pieceType,
                capturedPiece, specialMove, null, isCheck, isCheckmate, algebraicNotation);
    }

    /**
     * Record a move with all parameters
     */
    public void recordMove(Long gameId, int moveNumber, String playerColor, String fromPos, String toPos,
                           String pieceType, String capturedPiece, String specialMove,
                           String promotionPiece, boolean isCheck, boolean isCheckmate, String algebraicNotation) throws SQLException {
        ensureConnection();
        String query = """
            INSERT INTO moves (game_id, move_number, player_color, from_position, to_position,
                             piece_type, captured_piece, special_move, promotion_piece,
                             is_check, is_checkmate, algebraic_notation)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, gameId);
            stmt.setInt(2, moveNumber);
            stmt.setString(3, playerColor);
            stmt.setString(4, fromPos);
            stmt.setString(5, toPos);
            stmt.setString(6, pieceType);
            stmt.setString(7, capturedPiece);
            stmt.setString(8, specialMove);
            stmt.setString(9, promotionPiece);
            stmt.setBoolean(10, isCheck);
            stmt.setBoolean(11, isCheckmate);
            stmt.setString(12, algebraicNotation);
            stmt.executeUpdate();
        }

        // Update total moves count
        updateGameMoveCount(gameId);
    }

    private void updateGameMoveCount(Long gameId) throws SQLException {
        String query = "UPDATE games SET total_moves = (SELECT COUNT(*) FROM moves WHERE game_id = ?) WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, gameId);
            stmt.setLong(2, gameId);
            stmt.executeUpdate();
        }
    }

    public void endGame(Long gameId, String gameResult) throws SQLException {
        ensureConnection();
        String query = "UPDATE games SET game_result = ?, end_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, gameResult);
            stmt.setLong(2, gameId);
            stmt.executeUpdate();
        }

        // Generate and save PGN
        String pgn = generatePGN(gameId);
        savePGN(gameId, pgn);
    }

    private void savePGN(Long gameId, String pgn) throws SQLException {
        String query = "UPDATE games SET pgn_notation = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, pgn);
            stmt.setLong(2, gameId);
            stmt.executeUpdate();
        }
    }

    // PGN Generation
    public String generatePGN(Long gameId) throws SQLException {
        ensureConnection();
        StringBuilder pgn = new StringBuilder();

        // Get game info
        String gameQuery = """
            SELECT g.*, u1.username as white_player, u2.username as black_player
            FROM games g
            LEFT JOIN users u1 ON g.white_player_id = u1.id
            LEFT JOIN users u2 ON g.black_player_id = u2.id
            WHERE g.id = ?
            """;

        try (PreparedStatement stmt = connection.prepareStatement(gameQuery)) {
            stmt.setLong(1, gameId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                pgn.append("[Event \"Online Chess Game\"]\n");
                pgn.append("[Site \"Chess Server\"]\n");
                pgn.append("[Date \"").append(rs.getTimestamp("start_time").toLocalDateTime().toLocalDate()).append("\"]\n");
                pgn.append("[Round \"1\"]\n");
                pgn.append("[White \"").append(rs.getString("white_player")).append("\"]\n");
                pgn.append("[Black \"").append(rs.getString("black_player")).append("\"]\n");
                pgn.append("[Result \"").append(convertResultToPGN(rs.getString("game_result"))).append("\"]\n\n");
            }
        }

        // Get moves
        String movesQuery = "SELECT * FROM moves WHERE game_id = ? ORDER BY move_number, player_color";
        try (PreparedStatement stmt = connection.prepareStatement(movesQuery)) {
            stmt.setLong(1, gameId);
            ResultSet rs = stmt.executeQuery();

            int currentMoveNumber = 0;
            while (rs.next()) {
                int moveNumber = rs.getInt("move_number");
                String playerColor = rs.getString("player_color");
                String algebraicNotation = rs.getString("algebraic_notation");

                if (playerColor.equals("WHITE")) {
                    if (currentMoveNumber != moveNumber) {
                        currentMoveNumber = moveNumber;
                        pgn.append(moveNumber).append(". ");
                    }
                    pgn.append(algebraicNotation).append(" ");
                } else {
                    pgn.append(algebraicNotation).append(" ");
                }
            }
        }

        return pgn.toString().trim();
    }

    private String convertResultToPGN(String result) {
        return switch (result) {
            case "WHITE_WIN" -> "1-0";
            case "BLACK_WIN" -> "0-1";
            case "DRAW" -> "1/2-1/2";
            default -> "*";
        };
    }

    // Spectator Management
    public void addSpectator(Long gameId, Long userId) throws SQLException {
        ensureConnection();
        String query = "INSERT INTO game_spectators (game_id, user_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, gameId);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    public List<Long> getGameSpectators(Long gameId) throws SQLException {
        ensureConnection();
        List<Long> spectators = new ArrayList<>();
        String query = "SELECT user_id FROM game_spectators WHERE game_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, gameId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                spectators.add(rs.getLong("user_id"));
            }
        }
        return spectators;
    }

    // PGN File Management Methods (for PGNService)

    /**
     * Store PGN file information
     */
    public Long storePGNFile(String filePath, String content, Long importedBy) throws SQLException {
        ensureConnection();
        String query = "INSERT INTO pgn_files (file_path, content, imported_by) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, filePath);
            stmt.setString(2, content);
            stmt.setLong(3, importedBy);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                return keys.getLong(1);
            }
            throw new SQLException("Failed to get generated PGN file ID");
        }
    }

    /**
     * Get all games for export
     */
    public List<Long> getAllGameIds() throws SQLException {
        ensureConnection();
        List<Long> gameIds = new ArrayList<>();
        String query = "SELECT id FROM games ORDER BY start_time DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                gameIds.add(rs.getLong("id"));
            }
        }
        return gameIds;
    }

    // Utility methods
    private String hashPassword(String password) {
        // Simple hash for demo - use BCrypt in production
        return Integer.toString(password.hashCode());
    }

    /**
     * Close existing connection safely
     */




    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Warning: Error closing existing connection: " + e.getMessage());
        }
    }

    public void close() {
        closeConnection();
    }
}


//package io.gchape.github.service;
//
//import io.gchape.github.model.entity.User;
//import java.sql.*;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.time.LocalDateTime;
//
//// Database Service for Chess Game Recording
//public class DatabaseService {
//    // Modified connection string with better lock handling and unique database per instance
//    private static final String DB_URL = "jdbc:h2:mem:chess_db;MODE=MySQL;DB_CLOSE_DELAY=-1";    private static final String DB_USER = "sa";
//    private static final String DB_PASSWORD = "";
//    private DatabaseService databaseService;
//    private Connection connection;
//    private static volatile DatabaseService instance;
//    private static final Object lock = new Object();
//
//    private DatabaseService() {
//        // In your Server constructor or wherever you create DatabaseService
//        databaseService = DatabaseService.getInstance();
//    }
//
//    // Singleton pattern to ensure only one instance
//    public static DatabaseService getInstance() {
//        if (instance == null) {
//            synchronized (lock) {
//                if (instance == null) {
//                    instance = new DatabaseService();
//                }
//            }
//        }
//        return instance;
//    }
//
//    private void initializeDatabase() {
//        try {
//            // Force close any existing connections and clean up lock files
//            cleanupExistingConnections();
//
//            Class.forName("org.h2.Driver");
//
//            // Try to create connection with retries
//            int retries = 3;
//            SQLException lastException = null;
//
//            for (int i = 0; i < retries; i++) {
//                try {
//                    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
//                    connection.setAutoCommit(true);
//                    break;
//                } catch (SQLException e) {
//                    lastException = e;
//                    System.err.println("Connection attempt " + (i + 1) + " failed: " + e.getMessage());
//
//                    if (i < retries - 1) {
//                        // Wait and try cleanup again
//                        Thread.sleep(2000);
//                        cleanupExistingConnections();
//                    }
//                }
//            }
//
//            if (connection == null) {
//                throw new RuntimeException("Failed to create database connection after " + retries + " attempts", lastException);
//            }
//
//            createTables();
//
//            // Add shutdown hook for proper cleanup
//            addShutdownHook();
//
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to initialize database", e);
//        }
//    }
//
//    private void cleanupExistingConnections() {
//        try {
//            // Try to close any existing connections to the same database
//            try (Connection shutdownConn = DriverManager.getConnection(
//                    "jdbc:h2:./chess_db;SHUTDOWN=TRUE", DB_USER, DB_PASSWORD)) {
//                // This will close the database if it's open
//            } catch (SQLException e) {
//                // Ignore - database might not be open
//            }
//
//            // Clean up lock files if they exist
//            java.io.File lockFile = new java.io.File("chess_db.lock.db");
//            if (lockFile.exists()) {
//                lockFile.delete();
//            }
//
//            // Small delay to ensure cleanup is complete
//            Thread.sleep(1000);
//
//        } catch (Exception e) {
//            System.err.println("Warning: Could not clean up existing connections: " + e.getMessage());
//        }
//    }
//
//    private void addShutdownHook() {
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                if (connection != null && !connection.isClosed()) {
//                    connection.close();
//                }
//                // Force shutdown of H2 database
//                try (Connection shutdownConn = DriverManager.getConnection(
//                        "jdbc:h2:./chess_db;SHUTDOWN=TRUE", DB_USER, DB_PASSWORD)) {
//                    // This will properly close the database
//                }
//            } catch (SQLException e) {
//                // Ignore during shutdown
//            }
//        }));
//    }
//
//    private void createTables() throws SQLException {
//        String[] createTableQueries = {
//                """
//            CREATE TABLE IF NOT EXISTS users (
//                id BIGINT PRIMARY KEY AUTO_INCREMENT,
//                username VARCHAR(50) UNIQUE NOT NULL,
//                password VARCHAR(255) NOT NULL,
//                email VARCHAR(100) UNIQUE NOT NULL,
//                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                last_login TIMESTAMP
//            )
//            """,
//                """
//            CREATE TABLE IF NOT EXISTS games (
//                id BIGINT PRIMARY KEY AUTO_INCREMENT,
//                white_player_id BIGINT,
//                black_player_id BIGINT,
//                game_mode VARCHAR(20) NOT NULL,
//                game_result VARCHAR(20) DEFAULT 'ONGOING',
//                start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                end_time TIMESTAMP,
//                total_moves INT DEFAULT 0,
//                pgn_notation TEXT,
//                FOREIGN KEY (white_player_id) REFERENCES users(id),
//                FOREIGN KEY (black_player_id) REFERENCES users(id)
//            )
//            """,
//                """
//            CREATE TABLE IF NOT EXISTS moves (
//                id BIGINT PRIMARY KEY AUTO_INCREMENT,
//                game_id BIGINT NOT NULL,
//                move_number INT NOT NULL,
//                player_color VARCHAR(5) NOT NULL,
//                from_position VARCHAR(2) NOT NULL,
//                to_position VARCHAR(2) NOT NULL,
//                piece_type VARCHAR(10) NOT NULL,
//                captured_piece VARCHAR(10),
//                special_move VARCHAR(20),
//                promotion_piece VARCHAR(10),
//                is_check BOOLEAN DEFAULT FALSE,
//                is_checkmate BOOLEAN DEFAULT FALSE,
//                move_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                algebraic_notation VARCHAR(10),
//                FOREIGN KEY (game_id) REFERENCES games(id)
//            )
//            """,
//                """
//            CREATE TABLE IF NOT EXISTS game_spectators (
//                id BIGINT PRIMARY KEY AUTO_INCREMENT,
//                game_id BIGINT NOT NULL,
//                user_id BIGINT NOT NULL,
//                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                FOREIGN KEY (game_id) REFERENCES games(id),
//                FOREIGN KEY (user_id) REFERENCES users(id)
//            )
//            """,
//                """
//            CREATE TABLE IF NOT EXISTS pgn_files (
//                id BIGINT PRIMARY KEY AUTO_INCREMENT,
//                file_path VARCHAR(500) NOT NULL,
//                content TEXT NOT NULL,
//                imported_by BIGINT,
//                imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
//                FOREIGN KEY (imported_by) REFERENCES users(id)
//            )
//            """
//        };
//
//        for (String query : createTableQueries) {
//            try (Statement stmt = connection.createStatement()) {
//                stmt.execute(query);
//            }
//        }

//    public void ensureConnection() throws SQLException {
//        if (connection == null || connection.isClosed() || !connection.isValid(5)) {
//            System.out.println("Reconnecting to database...");
//            initializeDatabase();
//        }
//    }
//
//    public void close() {
//        closeConnection();
//    }
//}