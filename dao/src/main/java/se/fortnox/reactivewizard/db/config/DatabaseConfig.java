package se.fortnox.reactivewizard.db.config;

import se.fortnox.reactivewizard.config.Config;

@Config("database")
public class DatabaseConfig {

    private String url;
    private String user;
    private String password;
    private String schema;
    private int    maximumPoolSize       = 10;
    private long   connectionTimeout     = 30000;
    private long   idleTimeout           = 600000;
    private long   maxLifetime           = 1800000;
    private int    minimumIdle           = 1;
    private long   slowQueryLogThreshold = 5000;

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPoolSize() {
        return maximumPoolSize;
    }

    public void setPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public int getMinimumIdle() {
        return minimumIdle;
    }

    public void setMinimumIdle(int minimumIdle) {
        this.minimumIdle = minimumIdle;
    }

    public long getSlowQueryLogThreshold() {
        return slowQueryLogThreshold;
    }

    public void setSlowQueryLogThreshold(long slowQueryLogThreshold) {
        this.slowQueryLogThreshold = slowQueryLogThreshold;
    }
}
