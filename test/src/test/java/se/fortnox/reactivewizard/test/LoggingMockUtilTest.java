package se.fortnox.reactivewizard.test;

import org.apache.log4j.Appender;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

public class LoggingMockUtilTest {
    @Test
    public void shouldLogToMockAppender() throws NoSuchFieldException, IllegalAccessException {
        ClassWithLogger classWithLogger = new ClassWithLogger();
        Appender appender = LoggingMockUtil.createMockedLogAppender(ClassWithLogger.class);

        classWithLogger.doSomethingLogged();

        verify(appender).doAppend(matches(log -> {
            assertThat(log.getLevel().toString()).isEqualTo("INFO");
            assertThat(log.getMessage().toString()).contains("Information was logged");
        }));
        LoggingMockUtil.destroyMockedAppender(appender, ClassWithLogger.class);
    }

    /**
     * Closes and removes the appender. Should be called after you have verified the logging, with help of this class.
     *
     */
    @Test
    public void shouldBePossibleToDestroyMockAppender() throws NoSuchFieldException, IllegalAccessException {
        Appender appender = LoggingMockUtil.createMockedLogAppender(ClassWithLogger.class);

        LoggingMockUtil.destroyMockedAppender(appender, ClassWithLogger.class);

        Appender destroyedAppender = LoggingMockUtil.getLogger(ClassWithLogger.class).getAppender("mockAppender");
        assertThat(destroyedAppender).isNull();
    }
}

class ClassWithLogger {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingMockUtilTest.class);

    public void doSomethingLogged() {
        LOG.info("Information was logged");
    }
}
