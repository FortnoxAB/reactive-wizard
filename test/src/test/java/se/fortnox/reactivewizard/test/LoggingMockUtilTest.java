package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

@ExtendWith(LoggingVerifierExtension.class)
class LoggingMockUtilTest {
    LoggingVerifier loggingVerifier = new LoggingVerifier(LoggingMockUtil.class);

    @Test
    void shouldLogToMockAppender() {
        ClassWithLogger classWithLogger = new ClassWithLogger();
        Appender appender = LoggingMockUtil.createMockedLogAppender(ClassWithLogger.class);

        classWithLogger.doSomethingLogged();

        verify(appender)
            .append(matches(log -> {
                assertThat(log.getLevel())
                    .isEqualTo(Level.INFO);
                assertThat(log.getMessage()
                    .toString())
                    .contains("Information was logged");
            }));
        LoggingMockUtil.destroyMockedAppender(ClassWithLogger.class);
    }

    @Test
    void shouldThrowExceptionIfFieldLOGCannotBeFoundWhileCreatingMockAppender() {
        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy(() -> LoggingMockUtil.createMockedLogAppender(String.class))
            .withCauseInstanceOf(ReflectiveOperationException.class);
    }

    /**
     * Closes and removes the appender. Should be called after you have verified the logging, with help of this class.
     */
    @Test
    void shouldBePossibleToDestroyMockAppender() {
        LoggingMockUtil.createMockedLogAppender(ClassWithLogger.class);

        LoggingMockUtil.destroyMockedAppender(ClassWithLogger.class);

        Appender destroyedAppender = LoggingMockUtil.getLogger(ClassWithLogger.class)
            .getAppenders().get("mockAppender");
        assertThat(destroyedAppender)
            .isNull();
    }

    /**
     * Should be able to close a non initialized appender without exception.
     */
    @Test
    void shouldNotThrowExceptionWhenDestroyingNonInitializedAppender() {
        LoggingMockUtil.destroyMockedAppender(ClassWithLogger.class);
        loggingVerifier.verify(Level.WARN, "Tried to destroy a mocked appender on se.fortnox.reactivewizard.test.ClassWithLogger but none was mocked. Perhaps you set up the mockedLogAppender for a different class?");
    }

    @Test
    void shouldKeepExistingAppenders() {
        org.apache.logging.log4j.core.Logger logger = LoggingMockUtil.getLogger(ClassWithLogger.class);
        Map<String, Appender> existingAppenders = logger.getAppenders();
        assertThat(existingAppenders)
            .isNotEmpty();
        LoggingMockUtil.createMockedLogAppender(ClassWithLogger.class);
        assertThat(logger.getAppenders())
            .containsAllEntriesOf(existingAppenders)
            .containsKey("mockAppender")
            .hasSize(existingAppenders.size() + 1);
    }
}

class ClassWithLogger {
    static final Logger LOG = LoggerFactory.getLogger(LoggingMockUtilTest.class);

    public void doSomethingLogged() {
        LOG.info("Information was logged");
    }
}
