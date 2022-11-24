package se.fortnox.reactivewizard.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import rx.Observable;
import rx.observers.TestSubscriber;
import se.fortnox.reactivewizard.config.TestInjector;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.util.DebugUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DbProxyTest {

    MockDb mockDb;
    DbProxyTestDao dbProxyTestDao;

    @Test
    public void shouldReturnDataFromDbForQuery() throws SQLException {
        mockDb.addRowColumn(1, 1, "sql_val", String.class, "myname");
        assertThat(dbProxyTestDao.select("mykey").toBlocking().single().getSqlVal()).isEqualTo("myname");
        mockDb.verifySelect("select * from table where key=?", "mykey");

        verify(mockDb.getPreparedStatement()).close();
        verify(mockDb.getResultSet()).close();
        verify(mockDb.getConnection()).close();
    }

    @Test
    public void shouldThrowExceptionWhenDirectlySelectingNullValues() throws SQLException {
        mockDb.addRowColumn(1, 1, "sql_val", String.class, null);
        StepVerifier.create(dbProxyTestDao.selectSpecificColumn("mykey"))
            .expectErrorSatisfies((throwable -> {
                Throwable throwableToAssert = throwable;
                if (DebugUtil.IS_DEBUG) {
                    throwableToAssert = throwable.getCause();
                }
                assertThat(throwableToAssert)
                    .isInstanceOf(NullPointerException.class);

                assertThat(throwableToAssert.getMessage())
                    .isEqualTo("""
                        One or more of the values returned in the resultset of the following query was null:
                        select sql_val from table where key=:key
                          
                        Project Reactor does not allow emitting null values in a stream. Wrap the return value from the dao interface
                        in a 'wrapper' to solve the issue.
                        Example: 
                        record Wrapper(String nullableValue) {};
                            """);
            }))
            .verify();

        mockDb.verifySelect("select sql_val from table where key=?", "mykey");
        verify(mockDb.getPreparedStatement()).close();
        verify(mockDb.getResultSet()).close();
        verify(mockDb.getConnection()).close();
    }

    @Test
    public void shouldReuseTypeReference() {
        final JsonSerializerFactory jsonSerializerFactoryReal = new JsonSerializerFactory();
        final JsonSerializerFactory jsonSerializerFactory = spy(jsonSerializerFactoryReal);
        AtomicReference<TypeReference<?>> typeReferenceAtomicReference = new AtomicReference<>();

        when(jsonSerializerFactory.createStringSerializer(any(TypeReference.class))).thenAnswer(invocationOnMock -> {
            final TypeReference<?> argument = invocationOnMock.getArgument(0);
            typeReferenceAtomicReference.set(argument);
            return jsonSerializerFactoryReal.createStringSerializer(argument);
        });
        new DbProxy(new DatabaseConfig(), null, null, jsonSerializerFactory);

        final TypeReference<?> firstTypeReference = typeReferenceAtomicReference.get();
        new DbProxy(new DatabaseConfig(), null, null, jsonSerializerFactory);
        final TypeReference<?> secondTypeReference = typeReferenceAtomicReference.get();

        assertThat(firstTypeReference).isSameAs(secondTypeReference);
    }

    @Test
    public void shouldReturnDataFromDbForQueryWithFlux() throws SQLException {
        mockDb.addRowColumn(1, 1, "sql_val", String.class, "myname");
        assertThat(dbProxyTestDao.selectFlux("mykey").single().block().getSqlVal()).isEqualTo("myname");
        mockDb.verifySelect("select * from table where key=?", "mykey");

        verify(mockDb.getPreparedStatement()).close();
        verify(mockDb.getResultSet()).close();
        verify(mockDb.getConnection()).close();
    }

    @Test
    public void shouldUpdateDbWithInputAndReturnAffectedRows() throws SQLException {
        mockDb.setUpdatedRows(1);
        assertThat(dbProxyTestDao.update("mykey", "myval").toBlocking().single()).isEqualTo(1);
        mockDb.verifyUpdate("update table set val=? where key=?", "myval", "mykey");

        verify(mockDb.getPreparedStatement(), timeout(100)).close();
        verify(mockDb.getConnection(), timeout(100)).close();
    }

    @Test
    public void shouldSupportReturningKeysFromInsert() throws SQLException {
        mockDb.addUpdatedRowId(1L);
        assertThat(dbProxyTestDao.insert().toBlocking().single().getKey()).isEqualTo(1L);
        mockDb.verifyUpdateGeneratedKeys("insert into table");

        verify(mockDb.getPreparedStatement()).close();
        verify(mockDb.getResultSet()).close();
        verify(mockDb.getConnection(), timeout(100)).close();
    }

    @Test
    public void shouldSetLocalDateAsSqlDate() throws SQLException {
        mockDb.setUpdatedRows(1);
        LocalDate date = LocalDate.now();
        assertThat(dbProxyTestDao.insertLocalDate(date).toBlocking().single()).isEqualTo(1);
        verify(mockDb.getPreparedStatement()).setDate(1, java.sql.Date.valueOf(date));
    }

    @Test
    public void shouldSetLocalTimeAsSqlTime() throws SQLException {
        mockDb.setUpdatedRows(1);
        LocalTime time = LocalTime.now();
        assertThat(dbProxyTestDao.insertLocalTime(time).toBlocking().single()).isEqualTo(1);
        verify(mockDb.getPreparedStatement()).setTime(1, java.sql.Time.valueOf(time));
    }

    @Test
    public void shouldSetLocalDateTimeAsSqlTimeStamp() throws SQLException {
        mockDb.setUpdatedRows(1);
        LocalDateTime now = LocalDateTime.now();
        assertThat(dbProxyTestDao.insertLocalDateTime(now).toBlocking().single()).isEqualTo(1);
        verify(mockDb.getPreparedStatement()).setTimestamp(1, java.sql.Timestamp.valueOf(now));
    }

    @Test
    public void shouldSetYearMonthAsInteger() throws SQLException {
        mockDb.setUpdatedRows(1);
        YearMonth yearMonth = YearMonth.parse("2021-10");
        assertThat(dbProxyTestDao.insertYearMonth(yearMonth).toBlocking().single()).isEqualTo(1);
        verify(mockDb.getPreparedStatement()).setObject(1, 202110);
    }

    @Test
    public void shouldSetNullAsParam() throws SQLException {
        mockDb.addUpdatedRowId(1L);
        assertThat(dbProxyTestDao.insertWithGeneratedKey(null).toBlocking().single().getKey()).isEqualTo(1L);
        mockDb.verifyUpdate("insert into table (date) values (?)", new Object[]{null});
    }

    @Test
    public void shouldThrowExceptionForBadReturnTypeOnUpdate() {
        try {
            dbProxyTestDao.insertBadreturnType().toBlocking().single();
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Unsupported return type for Update");
            return;
        }
        fail("expected exception");
    }

    @Test
    public void shouldThrowExceptionMethodsWithoutAnnotation() {
        try {
            dbProxyTestDao.methodMissingAnnotation().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Missing annotation @Query or @Update required");
            return;
        }
        fail("expected exception");
    }

    @Test
    public void shouldReturnErrorWhenSQLFails() throws SQLException {
        when(mockDb.getPreparedStatement().executeQuery()).thenThrow(new SQLException("db-error"));
        try {
            dbProxyTestDao.failingSql().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("db-error");
            verify(mockDb.getConnection(), timeout(500)).close();
            verify(mockDb.getPreparedStatement()).close();
            verify(mockDb.getResultSet(), never()).close();
            return;
        }
        fail("expected exception");
    }

    @Test
    public void shouldReturnErrorWhenSQLFailsAndCopeWithThatCloseFails() throws SQLException {
        when(mockDb.getPreparedStatement().executeQuery()).thenThrow(new SQLException("db-error"));
        doThrow(new SQLException("db-error")).when(mockDb.getConnection()).close();
        try {
            dbProxyTestDao.failingSql().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).isEqualTo("db-error");
            verify(mockDb.getConnection(), timeout(500)).close();
            verify(mockDb.getPreparedStatement()).close();
            verify(mockDb.getResultSet(), never()).close();
            return;
        }
        fail("expected exception");
    }

    @Test
    public void shouldFailIfUpdateReturnsZeroAffectedRows() throws SQLException {
        mockDb.setUpdatedRows(0);
        try {
            dbProxyTestDao.update("mykey", "myval").toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).contains("Minimum affected rows not reached");
            assertThat(e.getCause().getMessage()).contains("Minimum: 1 actual: 0");
            return;
        }
        fail("expected exception");
    }

    @Test
    public void shouldFailIfUpdateReturnsZeroGeneratedKeys() throws SQLException {
        mockDb.setUpdatedRows(0);

        try {
            dbProxyTestDao.insert().toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).contains("Minimum affected rows not reached");
            assertThat(e.getCause().getMessage()).contains("Minimum: 1 actual: 0");
            return;
        }
        fail("expected exception");
    }

    @Test
    public void shouldCreateNewDbProxyInstanceWithNewConnectionProviderAndNewDatabaseConfig() {
        // given
        DatabaseConfig oldConfig = new DatabaseConfig();
        oldConfig.setUrl("fooo");

        DatabaseConfig newConfig = new DatabaseConfig();
        newConfig.setUrl("bar");

        // when
        DbProxy oldDbProxy = new DbProxy(oldConfig, mock(ConnectionProvider.class));

        DbProxy newDbProxy = oldDbProxy.usingConnectionProvider(mock(ConnectionProvider.class), newConfig);

        // then
        assertThat(oldDbProxy).isNotSameAs(newDbProxy);
        assertThat(newDbProxy.getDatabaseConfig()).isNotSameAs(oldConfig);
        assertThat(newDbProxy.getDatabaseConfig()).isSameAs(newConfig);
        assertThat(newDbProxy.getDatabaseConfig().getUrl()).isSameAs("bar");
    }

    @Test
    public void shouldCreateNewDbProxyInstanceWithNewConnectionProviderAndNewScheduler() {
        // given
        DatabaseConfig config = new DatabaseConfig();
        config.setUrl("fooo");

        ConnectionProvider newConnectionProvider = mock(ConnectionProvider.class);
        when(newConnectionProvider.get()).thenReturn(mockDb.getConnection());

        Scheduler newScheduler = mock(Scheduler.class);
        when(newScheduler.createWorker()).thenReturn(Schedulers.boundedElastic().createWorker());

        // when
        DbProxy oldDbProxy = new DbProxy(config, mock(ConnectionProvider.class));

        DbProxy newDbProxy = oldDbProxy.usingConnectionProvider(newConnectionProvider, newScheduler);
        newDbProxy.create(DbProxyTestDao.class).select("").toBlocking().subscribe();

        // then
        assertThat(oldDbProxy).isNotSameAs(newDbProxy);
        verify(newScheduler).createWorker();
        verify(newConnectionProvider).get();
    }

    @Test
    public void testAllPathsOfTryWithResourceUsingGeneratedKey() throws SQLException {
        // null AutoCloseable
        setup();
        mockDb.addUpdatedRowId(1L);
        when(mockDb.getConnection().prepareStatement(anyString(), anyInt())).thenReturn(null);
        insertAndAssertError(null);

        // Throws on insert
        setup();
        mockDb.addUpdatedRowId(1L);
        when(mockDb.getPreparedStatement().executeUpdate()).thenThrow(new SQLException("execute update"));
        insertAndAssertError("execute update");

        // Throws on close
        setup();
        mockDb.addUpdatedRowId(1L);
        doThrow(new SQLException("close")).when(mockDb.getPreparedStatement()).close();
        insertAndAssertError("close");

        // Throws on insert and close
        setup();
        mockDb.addUpdatedRowId(1L);
        when(mockDb.getPreparedStatement().executeUpdate()).thenThrow(new SQLException("execute update"));
        doThrow(new SQLException("close")).when(mockDb.getPreparedStatement()).close();
        insertAndAssertError("execute update");

        // Throws in resource specification
        setup();
        mockDb.addUpdatedRowId(1L);
        when(mockDb.getConnection().prepareStatement(anyString(), anyInt())).thenThrow(new SQLException("prepare statement"));
        insertAndAssertError("prepare statement");

        // null AutoCloseable
        setup();
        mockDb.addUpdatedRowId(1L);
        when(mockDb.getPreparedStatement().getGeneratedKeys()).thenReturn(null);
        insertAndAssertError(null);

        // Throws on resultset.next
        setup();
        mockDb.addUpdatedRowId(1L);
        Observable<GeneratedKey<Long>> resultsetError = dbProxyTestDao.insert()
            .concatMap(o -> Observable.error(new RuntimeException("next")));
        insertAndAssertError(resultsetError, "next");

        // Throws on close
        setup();
        mockDb.addUpdatedRowId(1L);
        doThrow(new SQLException("close")).when(mockDb.getResultSet()).close();
        insertAndAssertError("close");

        // Throws on next and close
        setup();
        mockDb.addUpdatedRowId(1L);
        doThrow(new SQLException("close")).when(mockDb.getResultSet()).close();
        Flux<GeneratedKey<Long>> r = dbProxyTestDao.insertFlux()
            .concatMap(o -> Flux.error(new RuntimeException("next")));
        insertAndAssertError(r, "next");

        // Throws in resource specification
        setup();
        mockDb.addUpdatedRowId(1L);
        when(mockDb.getPreparedStatement().getGeneratedKeys()).thenThrow(new SQLException("generated keys"));
        insertAndAssertError("generated keys");
    }

    @Test
    public void testAllPathsOfTryWithResourceUsingCount() throws SQLException {
        // null AutoCloseable
        setup();
        mockDb.setUpdatedRows(1);
        when(mockDb.getConnection().prepareStatement(anyString())).thenReturn(null);
        insertAndAssertError(insertCount(), null);

        // Throws on insert
        setup();
        mockDb.setUpdatedRows(1);
        when(mockDb.getPreparedStatement().executeUpdate()).thenThrow(new SQLException("execute update"));
        insertAndAssertError(insertCount(), "execute update");

        // Throws on close
        setup();
        mockDb.setUpdatedRows(1);
        doThrow(new SQLException("close")).when(mockDb.getPreparedStatement()).close();
        insertAndAssertError(insertCount(), "close");

        // Throws on insert and close
        setup();
        mockDb.setUpdatedRows(1);
        when(mockDb.getPreparedStatement().executeUpdate()).thenThrow(new SQLException("execute update"));
        doThrow(new SQLException("close")).when(mockDb.getPreparedStatement()).close();
        insertAndAssertError(insertCount(), "execute update");

        // Throws in resource specification
        setup();
        mockDb.setUpdatedRows(1);
        when(mockDb.getConnection().prepareStatement(anyString())).thenThrow(new SQLException("prepare statement"));
        insertAndAssertError(insertCount(), "prepare statement");
    }

    private Observable<Integer> insertCount() {
        return dbProxyTestDao.insertLocalDate(LocalDate.MAX);
    }

    private void insertAndAssertError(String message) {
        insertAndAssertError(dbProxyTestDao.insert(), message);
    }

    private void insertAndAssertError(Observable<?> insert, String message) {
        try {
            insert.toBlocking().singleOrDefault(null);
            fail("expected exception");
        } catch (Exception e) {
            Exception cause = (Exception) e.getCause();
            if (cause != null) {
                e = cause;
            }
            if (message != null) {
                assertThat(e.getMessage()).contains(message);
            }
        }
    }

    private void insertAndAssertError(Flux<?> insert, String message) {
        assertThatExceptionOfType(Exception.class)
            .isThrownBy(insert::blockLast)
            .satisfies(exception -> {
                Throwable selfOrCause = Optional.ofNullable(exception.getCause()).orElse(exception);
                assertThat(selfOrCause.getMessage()).contains(message);
            });
    }

    @Test
    public void shouldPassIfUpdateReturnsZeroAffectedRowsAndQueryAllowsZeroUpdates() throws SQLException {
        mockDb.setUpdatedRows(0);
        assertThatNoException()
            .isThrownBy(() -> dbProxyTestDao.updateAllowingZeroAffectedRows("mykey", "myval").toBlocking().singleOrDefault(null));
    }

    @Test
    public void shouldAllowVoidReturnType() throws SQLException {
        mockDb.setUpdatedRows(1);
        assertThat(dbProxyTestDao.insertVoidReturnType().isEmpty().toBlocking().single()).isTrue();
        verify(mockDb.getPreparedStatement()).executeUpdate();
    }

    @Test
    public void shouldFailIfMinimumAffectedRowsNotReachedForVoidReturnType() throws SQLException {
        mockDb.setUpdatedRows(0);
        try {
            dbProxyTestDao.insertVoidReturnType().isEmpty().toBlocking().single();
            fail("expected exception");
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).contains("Minimum affected rows not reached");
            assertThat(e.getCause().getMessage()).contains("Minimum: 1 actual: 0");
        }
        verify(mockDb.getPreparedStatement()).executeUpdate();
    }

    @Test
    public void shouldSupportBackpressure() throws SQLException, InterruptedException {
        mockDb.addRowColumn(1, 1, "name", String.class, "row1");
        mockDb.addRowColumn(2, 1, "name", String.class, "row2");
        TestSubscriber<DbTestObj> testSubscriber = new TestSubscriber<>(0);
        dbProxyTestDao.select("mykey").subscribe(testSubscriber);

        testSubscriber.requestMore(1);
        Thread.sleep(100);
        testSubscriber.assertValueCount(1);
        testSubscriber.assertNoTerminalEvent();

        verify(mockDb.getConnection()).close();
        verify(mockDb.getPreparedStatement()).close();
        verify(mockDb.getResultSet()).close();

        testSubscriber.requestMore(2);
        Thread.sleep(100);
        testSubscriber.assertValueCount(2);
        testSubscriber.assertCompleted();
    }

    @Test
    public void shouldReturnErrorOnFullBuffer() throws SQLException {
        mockDb.setRowCount(MockDb.INFINITE);
        mockDb.addRowColumn(-1, 1, "name", String.class, "row2");
        StepVerifier.create(dbProxyTestDao.selectFlux("mykey"), 1) // subscribe and request one
            .thenAwait(ofSeconds(5)) // wait for the buffer to fill up
            .thenRequest(100000) // request the whole buffer (ReactiveStatementFactory.RECORD_BUFFER_SIZE)
            .expectNextCount(100001)
            .verifyErrorMatches(Exceptions::isOverflow);
    }

    @Before
    public void setup() {
        mockDb = new MockDb();
        ConnectionProvider connectionProvider = mockDb.getConnectionProvider();
        Injector injector = TestInjector.create(binder ->
            binder.bind(ConnectionProvider.class).toInstance(connectionProvider));
        dbProxyTestDao = injector.getInstance(DbProxyTestDao.class);
    }
}

