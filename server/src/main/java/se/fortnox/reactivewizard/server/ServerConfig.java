package se.fortnox.reactivewizard.server;

import se.fortnox.reactivewizard.config.Config;

/**
 * Configuration for a server.
 */
@Config("server")
public class ServerConfig {
    private int port = 8080;
    private boolean enabled = true;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
