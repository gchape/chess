package io.gchape.github.model.entity;



import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity class representing a user in the chess application.
 */
public class User {
    private Long id;
    private String username;
    private String password; // Should be hashed in production
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private int gamesPlayed;
    private int gamesWon;
    private int gamesLost;
    private int gamesDraw;

    // Default constructor
    public User() {
        this.createdAt = LocalDateTime.now();
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.gamesDraw = 0;
    }

    // Constructor for registration
    public User(String username, String password, String email) {
        this();
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Constructor with ID (for database retrieval)
    public User(Long id, String username, String password, String email,
                LocalDateTime createdAt, LocalDateTime lastLogin,
                int gamesPlayed, int gamesWon, int gamesLost, int gamesDraw) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.gamesLost = gamesLost;
        this.gamesDraw = gamesDraw;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }

    public int gamesDraw() {
        return gamesDraw;
    }

    public void setGamesDraw(int gamesDraw) {
        this.gamesDraw = gamesDraw;
    }

    // Utility methods
    public void incrementGamesPlayed() {
        this.gamesPlayed++;
    }

    public void incrementGamesWon() {
        this.gamesWon++;
        incrementGamesPlayed();
    }

    public void incrementGamesLost() {
        this.gamesLost++;
        incrementGamesPlayed();
    }

    public void incrementGamesDraw() {
        this.gamesDraw++;
        incrementGamesPlayed();
    }

    public double getWinRate() {
        return gamesPlayed > 0 ? (double) gamesWon / gamesPlayed : 0.0;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", gamesPlayed=" + gamesPlayed +
                ", gamesWon=" + gamesWon +
                ", gamesLost=" + gamesLost +
                ", gamesDraw=" + gamesDraw +
                '}';
    }
}