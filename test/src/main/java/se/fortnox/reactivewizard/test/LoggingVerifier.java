package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.rules.ExternalResource;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

/**
 * Use as a @Rule annotated field in your unit tests to assert error logs.
 */
public class LoggingVerifier extends ExternalResource {
    private final Class<?> clazz;
    private Appender appender;

    public LoggingVerifier(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void before() throws Throwable {
        this.appender = LoggingMockUtil.createMockedLogAppender(this.clazz);
    }

    @Override
    protected void after() {
        LoggingMockUtil.destroyMockedAppender(this.clazz);
    }

    /**
     * Verify some error Message
     * @param level the level to assert
     * @param errorMessage the error message to assert
     */
    public void verify(Level level, String errorMessage) {
        this.verify(level, logEvent -> assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo(errorMessage));
    }

    /**
     * Verify logging is carried out at a certain level.
     * The logEventAsserter is responsible for asserting the correctness of the logEvent object
     * @param level the log level
     * @param logEventAsserter the log event asserter
     */
    public void verify(Level level, Consumer<LogEvent> logEventAsserter) {
        Mockito.verify(appender, Mockito.atLeastOnce()).append(matches(logEvent -> {
            if (level.equals(logEvent.getLevel())) {
                logEventAsserter.accept(logEvent);
            }
        }));
    }
}
