package se.fortnox.reactivewizard.dbmigrate;

import org.junit.Test;
import se.fortnox.reactivewizard.config.ConfigFactory;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiquibaseMigrateProviderTest {

    @Test
    public void shouldGetLiquibaseMigrateInstance() {
        LiquibaseConfig liquibaseConfig = mock(LiquibaseConfig.class);

        when(liquibaseConfig.getUrl()).thenReturn("jdbc:h2:mem:test");
        when(liquibaseConfig.getMigrationsFile()).thenReturn("migrations.xml");

        ConfigFactory configFactory = mock(ConfigFactory.class);
        when(configFactory.get(LiquibaseConfig.class)).thenReturn(liquibaseConfig);

        LiquibaseMigrateProvider liquibaseMigrateProvider = new LiquibaseMigrateProvider(configFactory);

        //Assert the liquibaseMigrateProvider is lazy
        verify(liquibaseConfig, never()).getUrl();

        LiquibaseMigrate liquibaseMigrate = liquibaseMigrateProvider.get();

        //Assert the url is used when we get the liquibaseMigrate objet
        verify(liquibaseConfig, atLeastOnce()).getUrl();
    }
}
