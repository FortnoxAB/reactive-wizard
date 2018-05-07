package se.fortnox.reactivewizard.db;

import com.codahale.metrics.Timer;
import org.apache.log4j.Appender;
import org.junit.Test;
import org.slf4j.MDC;
import rx.Observable;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.transactions.DaoTransactionsImpl;
import se.fortnox.reactivewizard.metrics.Metrics;

import java.sql.SQLException;
import java.util.SortedMap;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static se.fortnox.reactivewizard.LoggingMockUtil.createMockedLogAppender;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

public class LoggingTest {
    private final MockDb              mockDb          = new MockDb();
    private final DaoTransactionsImpl daoTransactions = new DaoTransactionsImpl();
    private final DatabaseConfig      databaseConfig  = new DatabaseConfig();
    private final DbProxy             dbProxy         = new DbProxy(databaseConfig, mockDb.getConnectionProvider());
    private final LoggingDao          loggingDao      = dbProxy.create(LoggingDao.class);

    @Test
    public void shouldGenerateMetrics() {
        loggingDao.doSomething("nisse").toBlocking().singleOrDefault(null);

        SortedMap<String, Timer> timers = Metrics.registry().getTimers();
        assertThat(timers.containsKey("DAO_type:query_method:se.fortnox.reactivewizard.db.LoggingTest$LoggingDao.doSomething_1")).isTrue();
    }

    @Test
    public void shouldTransferLog() {
        // Run a query to make the thread pool create a thread, so that MDC's
        // inherit behaviour does not kick in and give a false positive
        loggingDao.doSomething("nisse").defaultIfEmpty(null).toBlocking().single();

        MDC.put("test", "hej");

        loggingDao.doSomething("nisse").defaultIfEmpty(null).map(record -> {
            assertThat(MDC.get("test")).isEqualTo("hej");
            return record;
        }).toBlocking().single();
    }

    @Test
    public void shouldLogSlowQueries() throws SQLException, NoSuchFieldException, IllegalAccessException {
        databaseConfig.setSlowQueryLogThreshold(1);

        // Given
        Appender mockAppender = createMockedLogAppender(ObservableStatementFactory.class);
        when(mockDb.getPreparedStatement().executeQuery()).thenAnswer(i -> {
            Thread.sleep(2);
            return mockDb.getResultSet();
        });

        // When
        loggingDao.doSomething("hej").toBlocking().singleOrDefault(null);

        // Then
        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Slow query: select \\* from table where name=:name\n" +
                "args: \\[\"hej\"\\]\n" +
                "time: \\d+")
        ));
        verifyNoMoreInteractions(mockAppender);
    }

    @Test
    public void shouldLogSlowQueriesWhenNotTransaction() throws SQLException, NoSuchFieldException, IllegalAccessException {
        databaseConfig.setSlowQueryLogThreshold(1);

        // Given
        Appender mockAppender = createMockedLogAppender(ObservableStatementFactory.class);
        when(mockDb.getPreparedStatement().executeUpdate()).thenAnswer(i -> {
            Thread.sleep(2);
            return 1;
        });

        // When
        loggingDao.doSomeUpdate("hej").toBlocking().singleOrDefault(null);

        // Then
        verify(mockAppender).doAppend(matches(log ->
            assertThat(log.getMessage().toString()).matches("Slow query: update table set name=:name\n" +
                "args: \\[\"hej\"\\]\n" +
                "time: \\d+")
        ));
        verifyNoMoreInteractions(mockAppender);
    }

    @Test
    public void shouldNotLogSlowQueriesForTransactions() throws SQLException, NoSuchFieldException, IllegalAccessException {
        databaseConfig.setSlowQueryLogThreshold(1);

        // Given
        Appender mockAppender = createMockedLogAppender(ObservableStatementFactory.class);
        when(mockDb.getPreparedStatement().executeUpdate()).thenAnswer(i -> {
            Thread.sleep(2);
            return 1;
        });

        // When
        Observable<Integer> insert = loggingDao.doSomeInsert("hej");
        Observable<Integer> update = loggingDao.doSomeUpdate("hej");
        Observable<Integer> delete = loggingDao.doSomeDelete("hej");
        daoTransactions.createTransaction(insert, update, delete);
        insert.subscribe();

        // Then
        verifyNoMoreInteractions(mockAppender);
    }

    public interface LoggingDao {
        @Query("select * from table where name=:name")
        Observable<Integer> doSomething(String name);

        @Update("insert into table values(:name)")
        Observable<Integer> doSomeInsert(String name);

        @Update("update table set name=:name")
        Observable<Integer> doSomeUpdate(String name);

        @Update("delete from table where name=:name")
        Observable<Integer> doSomeDelete(String name);
    }
}
