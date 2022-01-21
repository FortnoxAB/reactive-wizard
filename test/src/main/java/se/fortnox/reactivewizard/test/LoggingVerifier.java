package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.rules.ExternalResource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use as a @Rule annotated field in your unit tests to assert logs att various levels.
 */
public class LoggingVerifier extends ExternalResource {
    private final Class<?> clazz;
    private final Level level;
    private Level originalLevel;
    private Appender appender;

    public LoggingVerifier(Class<?> clazz) {
        this(clazz, null);
    }

    public LoggingVerifier(Class<?> clazz, Level level) {
        this.clazz = clazz;
        this.level = level;
    }

    @Override
    protected void before() throws Throwable {
        if (this.level != null) {
            this.originalLevel = LogManager.getLogger(this.clazz).getLevel();
            LoggingMockUtil.setLevel(this.clazz, this.level);
        }
        this.appender = LoggingMockUtil.createMockedLogAppender(this.clazz);
    }

    @Override
    protected void after() {
        LoggingMockUtil.destroyMockedAppender(this.clazz);
        LoggingMockUtil.setLevel(this.clazz, originalLevel);
    }

    /**
     * Verify some error Message
     * @param level the level to assert
     * @param errorMessage the error message to assert
     */
    public void verify(Level level, String errorMessage) {
        this.verify(Mockito.atLeastOnce(), level, logEvent -> assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo(errorMessage));
    }

    /**
     * Verify some LogEvent is sent at least once
     * @param level the level to assert
     * @param logEventAsserter the consumer of a LogEvent responsible for asserting the values of the LogEvent
     */
    public void verify(Level level, Consumer<LogEvent> logEventAsserter) {
        this.verify(Mockito.atLeastOnce(), level, logEventAsserter);
    }

    /**
     * Verify logging is carried out at a certain level.
     * The logEventAsserter is responsible for asserting the correctness of the logEvent object
     * @param level the log level
     * @param logEventAsserter the log event asserter
     */
    public void verify(VerificationMode verificationMode, Level level, Consumer<LogEvent> logEventAsserter) {
        ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);

        Mockito.verify(appender, verificationMode).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getAllValues())
            .anySatisfy(logEvent -> {
                assertThat(logEvent.getLevel()).isEqualTo(level);
                logEventAsserter.accept(logEvent);
            });
    }
}
