package se.fortnox.reactivewizard.db.statement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.fortnox.reactivewizard.test.LoggingVerifier;
import se.fortnox.reactivewizard.test.LoggingVerifierExtension;

import java.sql.PreparedStatement;

import static org.mockito.Mockito.lenient;
import static se.fortnox.reactivewizard.db.statement.StatementDebug.log;

@ExtendWith({MockitoExtension.class, LoggingVerifierExtension.class})
class StatementDebugTest {

    @Mock
    PreparedStatement preparedStatement;

    LoggingVerifier loggingVerifier = new LoggingVerifier(StatementDebug.class);

    @BeforeEach
    void setupMocks() {
        lenient().doReturn("SELECT column FROM table WHERE column='abc'").when(preparedStatement).toString();
        loggingVerifier.before();
    }

    @Test
    void shouldNotLogByDefault() {
        log(preparedStatement);
        loggingVerifier.assertThatLogs().isEmpty();
    }
}
