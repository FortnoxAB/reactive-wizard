package se.fortnox.reactivewizard.db;

import org.junit.Test;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectionProviderTest {

    @Test
    public void shouldUseConnectionPool() {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setUrl("jdbc:h2:mem:test");
        ConnectionProviderImpl connectionProvider = new ConnectionProviderImpl(databaseConfig);
        try {
            assertThat(connectionProvider.get()).isNotNull();
        } finally {
            connectionProvider.close();
        }
    }
}
