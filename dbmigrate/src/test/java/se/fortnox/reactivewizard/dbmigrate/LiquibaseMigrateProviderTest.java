package se.fortnox.reactivewizard.dbmigrate;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import se.fortnox.reactivewizard.config.ConfigFactory;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiquibaseMigrateProviderTest {

    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer();

    @Test
    public void shouldGetLiquibaseMigrateInstance() {
        LiquibaseConfig liquibaseConfig = new LiquibaseConfig();

        liquibaseConfig.setUrl(postgreSQLContainer.getJdbcUrl());
        liquibaseConfig.setUser(postgreSQLContainer.getUsername());
        liquibaseConfig.setPassword(postgreSQLContainer.getPassword());

        liquibaseConfig = spy(liquibaseConfig);

        ConfigFactory configFactory = mock(ConfigFactory.class);
        when(configFactory.get(LiquibaseConfig.class)).thenReturn(liquibaseConfig);

        LiquibaseMigrateProvider liquibaseMigrateProvider = new LiquibaseMigrateProvider(configFactory);

        //Assert the liquibaseMigrateProvider is lazy
        verify(liquibaseConfig, never()).getUrl();

        liquibaseMigrateProvider.get();

        //Assert the url is used when we get the liquibaseMigrate objet
        verify(liquibaseConfig, atLeastOnce()).getUrl();
    }
}
