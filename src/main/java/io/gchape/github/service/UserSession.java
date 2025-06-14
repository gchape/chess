package io.gchape.github.service;

public class UserSession {
    private Long userId;
    private String username;

    public UserSession(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
}