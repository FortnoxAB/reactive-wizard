package se.fortnox.reactivewizard.logging;

import org.apache.log4j.Appender;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fest.assertions.Assertions.assertThat;
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
    }
}

class ClassWithLogger {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingMockUtilTest.class);

    public void doSomethingLogged() {
        LOG.info("Information was logged");
    }
}
