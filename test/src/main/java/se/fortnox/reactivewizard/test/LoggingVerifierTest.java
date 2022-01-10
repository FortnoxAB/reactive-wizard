package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class LoggingVerifierTest {

    //Unused it seems but is needed for the mocked appender to be set up properly
    private static Logger LOG = LoggerFactory.getLogger(LoggingVerifierTest.class);

    @Rule
    public LoggingVerifier loggingVerifier = new LoggingVerifier(LoggingVerifierTest.class);

    @Test
    public void verifyLogging() {
        LogManager.getLogger(LoggingVerifierTest.class).trace("trace");
        LogManager.getLogger(LoggingVerifierTest.class).debug("debug");
        LogManager.getLogger(LoggingVerifierTest.class).error("error");
        LogManager.getLogger(LoggingVerifierTest.class).warn("warn");
        LogManager.getLogger(LoggingVerifierTest.class).info("info");

        loggingVerifier.verify(Level.TRACE, "trace");
        loggingVerifier.verify(Level.DEBUG, "debug");
        loggingVerifier.verify(Level.ERROR, "error");
        loggingVerifier.verify(Level.WARN, "warn");
        loggingVerifier.verify(Level.INFO, logEvent -> assertThat(logEvent.getMessage().getFormattedMessage()).isEqualTo("info"));
    }
}
