package io.gchape.github.model.message;

public class LoginMessage extends Message {
    private String type = "LOGIN"; // Always include type field
    private String username;
    private String password;

    public LoginMessage() {}

    public LoginMessage(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getType() {
        return "LOGIN";
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}