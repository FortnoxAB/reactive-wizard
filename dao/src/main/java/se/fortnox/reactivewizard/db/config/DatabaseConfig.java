package se.fortnox.reactivewizard.db.config;

import se.fortnox.reactivewizard.config.Config;

import java.util.concurrent.Executor;

@Config("database")
public class DatabaseConfig {

    private String url;
    private String user;
    private String password;
    private String schema;
    private int    maximumPoolSize       = 10;
    
    /* Set connectionTimeout to 10s, which is a reasonable time before we 
       give up and attempt a new one. Useful for recovering more quickly 
       from network problems.
    */
    private long   connectionTimeout     = 10000;
    
    /* Set idleTimeout to 2m, to close idle connections more quickly than
       default. This avoids hogging connections that might be used for
       clients other than us.
    */
    private long   idleTimeout           = 120000;
    
    /* Set maxLifetime to 5m, to recycle connections older than that. This 
       helps when running db-proxy pods in kubernetes, since such pods will 
       be able to be more quickly rescheduled to other nodes (within 5 
       minutes rather than default 30). maxLifetime will however never 
       terminate an active connection, just recycle connections when they 
       become idle and have existed for longer than 5m.
    */
    private long   maxLifetime           = 300000;
    
    private int    minimumIdle           = 1;
    private long   slowQueryLogThreshold = 5000;
    private long   socketTimeout         = 300;

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

    /**
     * Get the configured socket timeout.
     * The socket timeout will be passed on to the jdbc driver and is a global timeout to stop absurdly long queries
     * or strange network partition problems. If the value is 0 the timeout is disabled.
     *
     * @see java.sql.Connection#setNetworkTimeout(Executor, int)
     * @return The socket timeout in seconds
     */
    public long getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(long socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}
