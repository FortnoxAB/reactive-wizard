package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingVerifierTest {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingVerifierTest.class);

    @Rule
    public LoggingVerifier loggingVerifier = new LoggingVerifier(LoggingVerifierTest.class);

    @Test
    public void verifyLogging() {
        LOG.trace("trace");
        LOG.debug("debug");
        LOG.error("error");
        LOG.warn("warn");
        LOG.info("info");

        loggingVerifier.verify(Level.TRACE, "trace");
        loggingVerifier.verify(Level.DEBUG, "debug");
        loggingVerifier.verify(Level.ERROR, "error");
        loggingVerifier.verify(Level.WARN, "warn");
        loggingVerifier.verify(Level.INFO, logEvent -> assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo("info"));
    }
}
