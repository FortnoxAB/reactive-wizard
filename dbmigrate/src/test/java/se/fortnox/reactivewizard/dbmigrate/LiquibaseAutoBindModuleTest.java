package se.fortnox.reactivewizard.dbmigrate;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.name.Names;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import org.junit.Test;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.server.ServerConfig;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LiquibaseAutoBindModuleTest {
    @Test
    public void testOnlyRunForDbArgument() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = getInjectedLiquibaseMock("run");

        verify(liquibaseMigrateMock, never()).drop();
        verify(liquibaseMigrateMock, never()).run();
        verify(liquibaseMigrateMock, never()).exit();
    }

    @Test
    public void testDropMigrate() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = getInjectedLiquibaseMock("db-drop-migrate");

        verify(liquibaseMigrateMock).drop();
        verify(liquibaseMigrateMock).run();
        verify(liquibaseMigrateMock).exit();
    }

    @Test
    public void testRunningNothing() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = getInjectedLiquibaseMock("db-run");

        verify(liquibaseMigrateMock, never()).drop();
        verify(liquibaseMigrateMock, never()).run();
        verify(liquibaseMigrateMock, never()).exit();
    }

    @Test
    public void testContinueEvenIfDropThrowsException() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        doThrow(new DatabaseException()).when(liquibaseMigrateMock).drop();

        getInjectedLiquibaseMock(liquibaseMigrateMock, "db-drop-migrate");

        verify(liquibaseMigrateMock).drop();
        verify(liquibaseMigrateMock).run();
        verify(liquibaseMigrateMock).exit();
    }

    @Test
    public void testRunThrowsException() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        doThrow(new LiquibaseException()).when(liquibaseMigrateMock).run();

        try {
            getInjectedLiquibaseMock(liquibaseMigrateMock, "db-drop-migrate");
        } catch (CreationException e ) {
            assertThat(e.getCause()).isInstanceOf(RuntimeException.class);
            assertThat(e.getCause().getCause()).isInstanceOf(LiquibaseException.class);
        }

        verify(liquibaseMigrateMock).drop();
        verify(liquibaseMigrateMock).run();
        verify(liquibaseMigrateMock, never()).exit();
    }



    @Test
    public void testSkipWhenArgumentIsNull() throws LiquibaseException {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        Guice.createInjector(new AutoBindModules(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            binder.bind(LiquibaseConfig.class).toInstance(new LiquibaseConfig() {{
                setUrl("jdbc:h2:mem:test");
            }});

            binder.bind(String[].class)
                .annotatedWith(Names.named("args"))
                .toInstance(new String[]{"config.yml"});

            binder.bind(LiquibaseMigrate.class).toInstance(liquibaseMigrateMock);
        }));

        verify(liquibaseMigrateMock, never()).drop();
        verify(liquibaseMigrateMock, never()).run();
        verify(liquibaseMigrateMock, never()).exit();
    }

    private LiquibaseMigrate getInjectedLiquibaseMock(String arg) {
        LiquibaseMigrate liquibaseMigrateMock = mock(LiquibaseMigrate.class);
        return getInjectedLiquibaseMock(liquibaseMigrateMock, arg);
    }

    private LiquibaseMigrate getInjectedLiquibaseMock(LiquibaseMigrate liquibaseMigrateMock, String arg) {
        Guice.createInjector(new AutoBindModules(binder -> {
            binder.bind(ServerConfig.class).toInstance(new ServerConfig() {{
                setEnabled(false);
            }});

            binder.bind(LiquibaseConfig.class).toInstance(new LiquibaseConfig() {{
                setUrl("jdbc:h2:mem:test");
            }});

            binder.bind(String[].class)
                .annotatedWith(Names.named("args"))
                .toInstance(new String[]{arg, null});

            binder.bind(LiquibaseMigrate.class).toInstance(liquibaseMigrateMock);
        }));

        return liquibaseMigrateMock;
    }

}
