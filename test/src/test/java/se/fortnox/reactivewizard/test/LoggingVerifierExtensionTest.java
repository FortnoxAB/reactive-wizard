package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.apache.logging.log4j.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(LoggingVerifierExtension.class)
class LoggingVerifierExtensionTest {

    static ClassThatLogsStuff classThatLogsStuff = new ClassThatLogsStuff();

    LoggingVerifier loggingVerifier = new LoggingVerifier(ClassThatLogsStuff.class);

    @Test
    void thatLoggingVerifierVerifiesIntendedClass() {
        classThatLogsStuff.logInfo("ping");
        classThatLogsStuff.logInfo("pong");
        loggingVerifier.verify(INFO, "ping");
        loggingVerifier.verify(INFO, "pong");
    }

    @AfterAll
    static void verifyLoggerDestroyed() {
        org.apache.logging.log4j.core.Logger logger = LoggingMockUtil.getLogger(ClassThatLogsStuff.class);
        assertThat(logger.getAppenders())
            .isEmpty();
    }
}
