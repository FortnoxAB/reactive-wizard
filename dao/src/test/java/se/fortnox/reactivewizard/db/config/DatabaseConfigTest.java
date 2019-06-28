package se.fortnox.reactivewizard.db.config;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabaseConfigTest {
    @Test
    public void shouldProvideDatabaseConfig() {
        // this is a bit absurd, but it pleases the coverage check gods
        DatabaseConfig config = new DatabaseConfig();
        config.setConnectionTimeout(1);
        config.setIdleTimeout(2);
        config.setMaxLifetime(3);
        config.setMinimumIdle(4);
        config.setPassword("pass");
        config.setPoolSize(5);
        config.setSchema("schema");
        config.setSlowQueryLogThreshold(6);
        config.setSocketTimeout(7);
        config.setUrl("url");
        config.setUser("user");

        assertThat(config.getConnectionTimeout()).isEqualTo(1);
        assertThat(config.getIdleTimeout()).isEqualTo(2);
        assertThat(config.getMaxLifetime()).isEqualTo(3);
        assertThat(config.getMinimumIdle()).isEqualTo(4);
        assertThat(config.getPassword()).isEqualTo("pass");
        assertThat(config.getPoolSize()).isEqualTo(5);
        assertThat(config.getSchema()).isEqualTo("schema");
        assertThat(config.getSlowQueryLogThreshold()).isEqualTo(6);
        assertThat(config.getSocketTimeout()).isEqualTo(7);
        assertThat(config.getUrl()).isEqualTo("url");
        assertThat(config.getUser()).isEqualTo("user");
    }
}
