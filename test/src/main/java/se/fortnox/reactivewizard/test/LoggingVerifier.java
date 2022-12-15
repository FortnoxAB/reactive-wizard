package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.assertj.core.api.ListAssert;
import org.junit.rules.ExternalResource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.WantedButNotInvoked;
import org.mockito.verification.VerificationMode;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;

/**
 * Use as a @Rule annotated field in your JUnit 4 tests to assert logs att various levels.
 * Use with <code>@ExtendWith(LoggingVerifierExtension.class)</code> on your JUnit 5 tests.
 */
public class LoggingVerifier extends ExternalResource {
    private final Class<?> clazz;
    private final Level level;
    private Level originalLevel;
    private Appender appender;

    /**
     * Create instance for class.
     *
     * @param clazz the class
     */
    public LoggingVerifier(Class<?> clazz) {
        this(clazz, null);
    }

    /**
     * Create instance for class and level.
     *
     * @param clazz the class
     * @param level the level
     */
    public LoggingVerifier(Class<?> clazz, Level level) {
        this.clazz = clazz;
        this.level = level;
    }

    @Override
    public void before() {
        if (this.level != null) {
            this.originalLevel = LogManager.getLogger(this.clazz).getLevel();
            LoggingMockUtil.setLevel(this.clazz, this.level);
        }
        this.appender = LoggingMockUtil.createMockedLogAppender(this.clazz);
    }

    @Override
    public void after() {
        LoggingMockUtil.destroyMockedAppender(this.clazz);
        LoggingMockUtil.setLevel(this.clazz, originalLevel);
    }

    /**
     * Verify some error Message.
     *
     * @param level        the level to assert
     * @param errorMessage the error message to assert
     */
    public void verify(Level level, String errorMessage) {
        this.verify(atLeastOnce(), level, logEvent -> assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo(errorMessage));
    }

    /**
     * Verify some LogEvent is sent at least once.
     *
     * @param level            the level to assert
     * @param logEventAsserter the consumer of a LogEvent responsible for asserting the values of the LogEvent
     */
    public void verify(Level level, Consumer<LogEvent> logEventAsserter) {
        this.verify(atLeastOnce(), level, logEventAsserter);
    }

    /**
     * Verify logging is carried out at a certain level.
     * The logEventAsserter is responsible for asserting the correctness of the logEvent object.
     *
     * @param verificationMode the verification mode
     * @param level            the log level
     * @param logEventAsserter the log event asserter
     */
    public void verify(VerificationMode verificationMode, Level level, Consumer<LogEvent> logEventAsserter) {
        assertThat(getLogEvents(verificationMode))
            .anySatisfy(logEvent -> {
                assertThat(logEvent.getLevel()).isEqualTo(level);
                logEventAsserter.accept(logEvent);
            });
    }

    /**
     * Get the mocked appender for use with Mockito.verify.
     *
     * @return the appender
     */
    public Appender getMockedAppender() {
        return appender;
    }

    /**
     * Returns a ListAssert of LogEvents that can be used for further assertions using AssertJ.
     *
     * @return a ListAssert of LogEvents
     */
    public ListAssert<LogEvent> assertThatLogs() {
        List<LogEvent> logEvents;
        try {
            logEvents = getLogEvents(atLeastOnce());
        } catch (WantedButNotInvoked wantedButNotInvoked) {
            logEvents = List.of();
        }
        return assertThat(logEvents);
    }

    private List<LogEvent> getLogEvents(VerificationMode verificationMode) {
        ArgumentCaptor<LogEvent> argumentCaptor = ArgumentCaptor.forClass(LogEvent.class);
        Mockito.verify(appender, verificationMode)
            .append(argumentCaptor.capture());
        return argumentCaptor.getAllValues();
    }
}
