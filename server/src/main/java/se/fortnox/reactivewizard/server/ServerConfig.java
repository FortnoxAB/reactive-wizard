package se.fortnox.reactivewizard.server;

import se.fortnox.reactivewizard.config.Config;

/**
 * Configuration for a server.
 */
@Config("server")
public class ServerConfig {
    private int     port                        = 8080;
    private boolean enabled                     = true;
    private int     maxHeaderSize               = 20 * 1024;
    private int     maxInitialLineLengthDefault = 4096;
    private int     maxRequestSize              = 10 * 1024 * 1024;
    private int     shutdownTimeoutSeconds      = 20;
    private boolean enableGzip                  = false;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxHeaderSize() {
        return maxHeaderSize;
    }

    public void setMaxHeaderSize(int maxHeaderSize) {
        this.maxHeaderSize = maxHeaderSize;
    }

    public int getMaxInitialLineLengthDefault() {
        return maxInitialLineLengthDefault;
    }

    public void setMaxInitialLineLengthDefault(int maxInitialLineLengthDefault) {
        this.maxInitialLineLengthDefault = maxInitialLineLengthDefault;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(int maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getShutdownTimeoutSeconds() {
        return shutdownTimeoutSeconds;
    }

    public void setShutdownTimeout(int shutdownTimeoutSeconds) {
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
    }

    public boolean isEnableGzip() {
        return enableGzip;
    }

    public void setEnableGzip(boolean enableGzip) {
        this.enableGzip = enableGzip;
    }
}
