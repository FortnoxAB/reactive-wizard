package se.fortnox.reactivewizard.client;

import se.fortnox.reactivewizard.config.Config;

@Config("test")
public class TestConfig {
    private String user;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
