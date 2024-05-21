package se.fortnox.reactivewizard.db;

import com.google.common.base.Strings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.metrics.Metrics;

import java.sql.Connection;
import java.sql.SQLException;

@Singleton
public class ConnectionProviderImpl implements ConnectionProvider {
    private final HikariDataSource ds;
    private final DatabaseConfig   databaseConfig;

    @Inject
    public ConnectionProviderImpl(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
        HikariConfig connectionPool = new HikariConfig();

        connectionPool.setJdbcUrl(databaseConfig.getUrl());
        connectionPool.setUsername(databaseConfig.getUser());
        connectionPool.setPassword(databaseConfig.getPassword());
        connectionPool.setMaximumPoolSize(databaseConfig.getPoolSize());
        connectionPool.setMinimumIdle(databaseConfig.getMinimumIdle());
        connectionPool.setIdleTimeout(databaseConfig.getIdleTimeout());
        connectionPool.setConnectionTimeout(databaseConfig.getConnectionTimeout());
        connectionPool.setMaxLifetime(databaseConfig.getMaxLifetime());
        connectionPool.setMetricRegistry(Metrics.registry());

        connectionPool.addDataSourceProperty("socketTimeout", databaseConfig.getSocketTimeout());
        if (!Strings.isNullOrEmpty(databaseConfig.getTimeZone())) {
            // The statement SET TIME ZONE is not a real table operation;
            // rather, it is a configuration statement that sets the time zone for the session.
            connectionPool.setConnectionInitSql("SET TIME ZONE '%s'".formatted(databaseConfig.getTimeZone()));
        }

        DbDriver.loadDriver(databaseConfig.getUrl());

        ds = new HikariDataSource(connectionPool);
    }

    @Override
    public Connection get() {
        try {
            Connection connection = ds.getConnection();
            if (databaseConfig.getSchema() != null) {
                connection.setSchema(databaseConfig.getSchema());
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        ds.close();
    }
}
