package se.fortnox.reactivewizard.dbmigrate;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.name.Names;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import org.junit.Test;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.server.ServerConfig;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiquibaseAutoBindModuleTest {
    @Test
    public void shouldOnlyRun() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = getInjectedLiquibaseMock("run");

        verify(liquibaseMigrateMock, never()).drop();
        verify(liquibaseMigrateMock, never()).run();
        verify(liquibaseMigrateMock, never()).exit();
    }

    @Test
    public void shouldDropMigrateAndExit() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = getInjectedLiquibaseMock("db-drop-migrate", null);

        verify(liquibaseMigrateMock).drop();
        verify(liquibaseMigrateMock).run();
        verify(liquibaseMigrateMock).exit();
    }

    @Test
    public void shouldNotExecuteLiquibase() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = getInjectedLiquibaseMock("db-run");

        verify(liquibaseMigrateMock, never()).drop();
        verify(liquibaseMigrateMock, never()).run();
        verify(liquibaseMigrateMock, never()).exit();
    }

    @Test
    public void shouldContinueRunningAfterDropThrowsException() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        doThrow(new DatabaseException()).when(liquibaseMigrateMock).drop();

        getInjectedLiquibaseMock(liquibaseMigrateMock, "db-drop-migrate", null);

        verify(liquibaseMigrateMock).drop();
        verify(liquibaseMigrateMock).run();
        verify(liquibaseMigrateMock).exit();
    }

    @Test
    public void shouldAbortIfRunThrowsException() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        doThrow(new LiquibaseException()).when(liquibaseMigrateMock).run();

        try {
            getInjectedLiquibaseMock(liquibaseMigrateMock, "db-drop-migrate", null);
            fail("Expected CreationException, but none was thrown");
        } catch (CreationException e) {
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(LiquibaseException.class);
        }

        verify(liquibaseMigrateMock).drop();
        verify(liquibaseMigrateMock).run();
        verify(liquibaseMigrateMock, never()).exit();
    }

    @Test
    public void shouldNotRunLiquibaseWhenArgCountIsOne() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        getInjectedLiquibaseMock(liquibaseMigrateMock, "config.yml");

        verify(liquibaseMigrateMock, never()).drop();
        verify(liquibaseMigrateMock, never()).run();
        verify(liquibaseMigrateMock, never()).exit();
    }

    private LiquibaseMigrate getInjectedLiquibaseMock(String... arg) {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        return getInjectedLiquibaseMock(liquibaseMigrateMock, arg);
    }

    private LiquibaseMigrate getInjectedLiquibaseMock(LiquibaseMigrate liquibaseMigrateMock, String... arg) {
        Guice.createInjector(new AutoBindModules(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            LiquibaseConfig liquibaseConfig = new LiquibaseConfig();
            liquibaseConfig.setUrl("jdbc:h2:mem:test");

            ConfigFactory configFactory = mock(ConfigFactory.class);
            when(configFactory.get(eq(LiquibaseConfig.class))).thenReturn(liquibaseConfig);
            binder.bind(ConfigFactory.class).toInstance(configFactory);

            binder.bind(String[].class)
                .annotatedWith(Names.named("args"))
                .toInstance(arg);

            LiquibaseMigrateProvider liquibaseMigrateProvider = mock(LiquibaseMigrateProvider.class);
            when(liquibaseMigrateProvider.get()).thenReturn(liquibaseMigrateMock);
            binder.bind(LiquibaseMigrateProvider.class).toInstance(liquibaseMigrateProvider);
        }));

        return liquibaseMigrateMock;
    }

}
