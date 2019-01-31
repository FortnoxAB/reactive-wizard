package se.fortnox.reactivewizard.dbmigrate;

import liquibase.exception.LiquibaseException;
import org.junit.Test;
import se.fortnox.reactivewizard.config.ConfigFactory;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LiquibaseMigrateProviderTest {

    @Test
    public void shouldGetLiquibaseMigrateInstance() throws IOException, LiquibaseException {
        LiquibaseConfig liquibaseConfig = new LiquibaseConfig();
        liquibaseConfig.setUrl("jdbc:h2:mem:test");

        ConfigFactory configFactory = mock(ConfigFactory.class);
        when(configFactory.get(LiquibaseConfig.class)).thenReturn(liquibaseConfig);

        LiquibaseMigrateProvider liquibaseMigrateProvider = new LiquibaseMigrateProvider(configFactory);

        LiquibaseMigrate liquibaseMigrate = liquibaseMigrateProvider.get();
    }
}
