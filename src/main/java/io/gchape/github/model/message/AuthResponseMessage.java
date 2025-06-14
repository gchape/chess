package io.gchape.github.model.message;

public class AuthResponseMessage extends Message {
    private String type = "AUTH_RESPONSE";

    private boolean success;
    private String message;
    private Long userId;
    private String username;

    public AuthResponseMessage() {}

    public AuthResponseMessage(boolean success, String message, Long userId, String username) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
    }

    @Override
    public String getType() {
        return "AUTH_RESPONSE";
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
