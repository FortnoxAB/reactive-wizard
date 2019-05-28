package se.fortnox.reactivewizard.client;

public class BasicAuthConfig {
    private String username;
    private String password;

    public BasicAuthConfig() {
    }

    public String getUsername() {
        return username;
    }

    public BasicAuthConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public BasicAuthConfig setPassword(String password) {
        this.password = password;
        return this;
    }
}
