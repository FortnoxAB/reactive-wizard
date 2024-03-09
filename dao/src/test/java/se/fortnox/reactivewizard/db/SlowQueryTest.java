package se.fortnox.reactivewizard.db;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.test.LoggingVerifier;
import se.fortnox.reactivewizard.test.LoggingVerifierExtension;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.apache.logging.log4j.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingVerifierExtension.class)
class SlowQueryTest {

    private final MockDb              mockDb             = new MockDb();
    private final DatabaseConfig      databaseConfig     = new DatabaseConfig();
    private final ConnectionProvider  connectionProvider = mockDb.getConnectionProvider();
    private final DbProxy             dbProxy            = new DbProxy(databaseConfig, connectionProvider);
    private final DbProxyTestDao      testDao            = dbProxy.create(DbProxyTestDao.class);

    private final LoggingVerifier loggingVerifier = new LoggingVerifier(ReactiveStatementFactory.class);

    @Test
    void shouldLogSlowQueries() throws SQLException {
        databaseConfig.setSlowQueryLogThreshold(1);

        // Given
        when(mockDb.getPreparedStatement().executeQuery()).thenAnswer(i -> {
            Awaitility.await().atMost(2, TimeUnit.MICROSECONDS);
            return mockDb.getResultSet();
        });

        // When
        testDao.select("hej").block();

        // Then
        loggingVerifier.verify(WARN, log ->
            assertThat(log.getMessage().getFormattedMessage())
                .contains("Slow execution: DAO_type:query_method:se.fortnox.reactivewizard.db.DbProxyTestDao.select_1 time: ")
        );
    }
}
