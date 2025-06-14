package io.gchape.github.model.message;

public class RegisterMessage extends Message {
    private String type = "REGISTER"; // Always include type field
    private String username;
    private String password;
    private String email;

    public RegisterMessage() {}

    public RegisterMessage(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public String getType() {
        return "REGISTER";
    }

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}