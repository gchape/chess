package io.gchape.github.model.message;

public class AuthResult {

    private boolean success;
    private String message;
    private Long userId;

    public AuthResult(boolean success, String message, Long userId) {
        this.success = success;
        this.message = message;
        this.userId = userId;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Long getUserId() { return userId; }
}
