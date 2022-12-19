package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.logging.log4j.Level.DEBUG;
import static org.apache.logging.log4j.Level.ERROR;
import static org.apache.logging.log4j.Level.INFO;
import static org.apache.logging.log4j.Level.TRACE;
import static org.apache.logging.log4j.Level.WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.never;

public class LoggingVerifierTest {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingVerifierTest.class);

    @Rule
    public LoggingVerifier loggingVerifier = new LoggingVerifier(LoggingVerifierTest.class, TRACE);

    @Test
    public void verifyLogging() {
        LOG.trace("trace");
        LOG.debug("debug");
        LOG.info("info");
        LOG.warn("warn");
        LOG.error("error");

        loggingVerifier.verify(TRACE, "trace");
        loggingVerifier.verify(DEBUG, "debug");
        loggingVerifier.verify(INFO, logEvent -> assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo("info"));
        loggingVerifier.verify(WARN, "warn");
        loggingVerifier.verify(ERROR, "error");

        Mockito.verify(loggingVerifier.getMockedAppender(), never()).append(TestUtil.matches(logEvent -> {
            assertThat(logEvent.getLevel()).isEqualTo(INFO);
            assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo("Test");
        }));
    }

    @Test
    public void shouldReturnListAssertOfLogEvents() {
        LOG.trace("trace");
        LOG.debug("debug");
        LOG.info("info");
        LOG.warn("warn");
        LOG.error("error");

        loggingVerifier.assertThatLogs()
            .extracting(LogEvent::getLevel, logEvent -> logEvent.getMessage().getFormattedMessage())
            .containsExactly(
                tuple(TRACE, "trace"),
                tuple(DEBUG, "debug"),
                tuple(INFO, "info"),
                tuple(WARN, "warn"),
                tuple(ERROR, "error")
            );
    }

    @Test
    public void shouldReturnEmptyListAssertIfNothingIsLogged() {
        loggingVerifier.assertThatLogs()
            .isEmpty();
    }

    @Test
    public void verifyLoggerDestroyedOnAfter() {
        loggingVerifier.after();
        org.apache.logging.log4j.core.Logger logger = LoggingMockUtil.getLogger(LoggingVerifierTest.class);
        assertThat(logger.getAppenders())
            .isEmpty();
    }
}
