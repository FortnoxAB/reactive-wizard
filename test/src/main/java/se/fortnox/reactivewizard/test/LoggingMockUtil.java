package se.fortnox.reactivewizard.test;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.slf4j.impl.Log4jLoggerAdapter;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoggingMockUtil {
    private LoggingMockUtil() {

    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static Appender createMockedLogAppender(Class cls) throws NoSuchFieldException, IllegalAccessException {
        Logger   logger       = LoggingMockUtil.getLogger(cls);
        Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("mockAppender");
        logger.addAppender(mockAppender);
        return mockAppender;
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static void destroyMockedAppender(Appender appender, Class cls) throws NoSuchFieldException, IllegalAccessException {
        Logger logger = LoggingMockUtil.getLogger(cls);
        appender.close();
        logger.removeAppender(appender);
    }

    /**
     * This unorthodox reflection magic is needed because the static logger of the ObservableStatementFactory may or may
     * not be initialized with the current LogManager, depending on the tests that have been run before.
     *
     * @return the logger instance used in the class
     */
    static Logger getLogger(Class cls) throws NoSuchFieldException, IllegalAccessException {
        Field logField = cls.getDeclaredField("LOG");
        logField.setAccessible(true);
        Log4jLoggerAdapter loggerAdapter = (Log4jLoggerAdapter)logField.get(null);
        Field              innerLogField = loggerAdapter.getClass().getDeclaredField("logger");
        innerLogField.setAccessible(true);
        return (Logger)innerLogField.get(loggerAdapter);
    }
}
