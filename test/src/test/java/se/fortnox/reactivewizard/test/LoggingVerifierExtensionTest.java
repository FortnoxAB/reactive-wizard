package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.apache.logging.log4j.Level.INFO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

@ExtendWith(LoggingVerifierExtension.class)
class LoggingVerifierExtensionTest {

    static ClassThatLogsStuff classThatLogsStuff = new ClassThatLogsStuff();

    @LoggingVerifierFor(ClassThatLogsStuff.class)
    LoggingVerifier loggingVerifier;

    @Test
    void thatLoggingVerifierIsInjected() {
        assertThat(loggingVerifier)
            .isNotNull();
    }

    @Test
    void thatLoggingVerifierVerifiesIntendedClass() {
        classThatLogsStuff.logInfo("ping");
        classThatLogsStuff.logInfo("pong");
        loggingVerifier.verify(INFO, "ping");
        loggingVerifier.verify(INFO, "pong");
    }

    @Test
    void thatAnnotatedStaticFieldProducesError() {
        LoggingVerifierExtension extension = new LoggingVerifierExtension();
        assertThatExceptionOfType(ExtensionConfigurationException.class)
            .isThrownBy(() -> extension.postProcessTestInstance(new WithStaticField(), mock(ExtensionContext.class)))
            .withMessage("Fields annotated with @LoggingVerifierFor must not be static");
    }

    @Test
    void thatAnnotatedFieldOfWrongTypeProducesError() {
        LoggingVerifierExtension extension = new LoggingVerifierExtension();
        assertThatExceptionOfType(ExtensionConfigurationException.class)
            .isThrownBy(() -> extension.postProcessTestInstance(new WithFieldOfWrongType(), mock(ExtensionContext.class)))
            .withMessage("Fields annotated with @LoggingVerifierFor must be of type LoggingVerifier");
    }

    @AfterAll
    static void verifyLoggerDestroyed() {
        org.apache.logging.log4j.core.Logger logger = LoggingMockUtil.getLogger(ClassThatLogsStuff.class);
        assertThat(logger.getAppenders())
            .isEmpty();
    }

    static class WithStaticField {
        @LoggingVerifierFor(ClassThatLogsStuff.class)
        static LoggingVerifier loggingVerifier;
    }

    static class WithFieldOfWrongType {
        @LoggingVerifierFor(ClassThatLogsStuff.class)
        static String loggingVerifier;
    }
}
