package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;

public class LoggingVerifierTest {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingVerifierTest.class);

    @Rule
    public LoggingVerifier loggingVerifier = new LoggingVerifier(LoggingVerifierTest.class, Level.TRACE);

    @Test
    public void verifyLogging() {
        LOG.trace("trace");
        LOG.debug("debug");
        LOG.info("info");
        LOG.warn("warn");
        LOG.error("error");

        loggingVerifier.verify(Level.TRACE, "trace");
        loggingVerifier.verify(Level.DEBUG, "debug");
        loggingVerifier.verify(Level.INFO, logEvent -> assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo("info"));
        loggingVerifier.verify(Level.WARN, "warn");
        loggingVerifier.verify(Level.ERROR, "error");

        Mockito.verify(loggingVerifier.getMockedAppender(), never()).append(TestUtil.matches(logEvent -> {
            assertThat(logEvent.getLevel()).isEqualTo(Level.INFO);
            assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo("Test");
        }));
    }

    @AfterClass
    public static void verifyLoggerDestroyed() {
        org.apache.logging.log4j.core.Logger logger = LoggingMockUtil.getLogger(LoggingInfoVerifierTest.class);
        assertThat(logger.getAppenders())
            .isEmpty();
    }
}
