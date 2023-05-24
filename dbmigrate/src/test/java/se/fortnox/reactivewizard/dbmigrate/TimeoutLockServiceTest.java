package se.fortnox.reactivewizard.dbmigrate;

import liquibase.Scope;
import liquibase.database.core.MockDatabase;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.ext.TimeoutLockService;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.LockDatabaseChangeLogStatement;
import liquibase.statement.core.UpdateStatement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeoutLockServiceTest {
    private MockDatabase                 database           = new MockDatabase();
    private Executor                     executor           = mock(Executor.class);
    private TimeoutLockService           timeoutLockService = new TimeoutLockService(10);
    private ArgumentCaptor<SqlStatement> argument           = ArgumentCaptor.forClass(SqlStatement.class);

    @Test
    void shouldRenewLockWhenProcessingTakesLongTime() throws LockException, DatabaseException {
        // When we aquire a lock
        timeoutLockService.acquireLock();

        // And wait until 2 update statements have been sent
        verify(executor, timeout(1000).atLeast(2)).update(argument.capture());

        // There should be a lock
        assertThat(argument.getAllValues().get(0)).isInstanceOf(LockDatabaseChangeLogStatement.class);

        // And then there should be a renewal
        assertThat(argument.getAllValues().get(1)).isInstanceOf(UpdateStatement.class);
        UpdateStatement renewalStatement = (UpdateStatement)argument.getAllValues().get(1);
        assertThat(renewalStatement.getNewColumnValues().get("LOCKGRANTED")).isInstanceOf(Date.class);

        // When we release the lock....
        timeoutLockService.releaseLock();

        // Then the renewals should have stopped
        reset(executor);
        verify(executor, after(500).never()).update(any());
    }

    @BeforeEach
    public void setup() throws DatabaseException {
        TimeoutLockService.setRenewalConnectionCreator(() -> database);

        when(executor.queryForObject(any(), any())).thenReturn(Boolean.FALSE);
        when(executor.update(any())).thenReturn(1);
        ExecutorService executorService = Scope.getCurrentScope().getSingleton(ExecutorService.class);
        executorService.setExecutor("jdbc", database, executor);

        timeoutLockService.setDatabase(database);
    }
}
