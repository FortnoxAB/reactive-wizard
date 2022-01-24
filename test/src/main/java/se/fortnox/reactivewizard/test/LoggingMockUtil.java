package se.fortnox.reactivewizard.test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.slf4j.Log4jLogger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Util for verifying logging in tests.
 */
public class LoggingMockUtil {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(LoggingMockUtil.class);

    private static final String MOCK_APPENDER = "mockAppender";

    private LoggingMockUtil() {

    }

    /**
     * Create mocked log appender.
     *
     * @param cls class for which to create appender
     * @return appender
     */
    public static org.apache.logging.log4j.core.Appender createMockedLogAppender(Class cls) {
        setLevel(Level.INFO);
        Logger logger       = LoggingMockUtil.getLogger(cls);

        Appender mockAppender = logger.getAppenders().get(MOCK_APPENDER);
        if (mockAppender != null) {
            return ((AppenderPreservingEvents)mockAppender).getInner();
        }

        mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn(MOCK_APPENDER);
        logger.addAppender(new AppenderPreservingEvents(mockAppender));
        return mockAppender;
    }

    /**
     * Destroy mocked log appender.
     * @param cls class for which to destroy appender
     */
    public static void destroyMockedAppender(Class cls) {
        Logger logger = LoggingMockUtil.getLogger(cls);
        Appender appender = logger.getAppenders().get(MOCK_APPENDER);

        if (appender != null) {
            logger.removeAppender(appender);
            return;
        }
        LOG.warn("Tried to destroy a mocked appender on " + cls.getName() +
            " but none was mocked. Perhaps you set up the mockedLogAppender for a different class?");
    }

    /**
     * This unorthodox reflection magic is needed because the static logger of the ObservableStatementFactory may or may
     * not be initialized with the current LogManager, depending on the tests that have been run before.
     *
     * @return the logger instance used in the class
     */
    static Logger getLogger(Class cls) {
        try {
            Field logField = cls.getDeclaredField("LOG");
            logField.setAccessible(true);
            Log4jLogger loggerAdapter = (Log4jLogger)logField.get(null);
            Field innerLogField = loggerAdapter.getClass()
                .getDeclaredField("logger");
            innerLogField.setAccessible(true);
            return (Logger)innerLogField.get(loggerAdapter);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Set root level.
     * @param level level
     */
    public static void setLevel(Level level) {
        Configurator.setRootLevel(level);
    }

    /**
     * Set level for class.
     * @param cls class
     * @param level level
     * @return old level
     */
    public static Level setLevel(Class<?> cls, Level level) {
        org.apache.logging.log4j.Logger logger = LogManager.getLogger(cls);
        Level oldLevel = logger.getLevel();
        Configurator.setLevel(logger.getName(), level);
        return oldLevel;
    }

    /**
     * Needed in order to propagate immutable LogEvent to mock. Otherwise, mockito will see a LogEvent that has been
     * mutated.
     */
    private static class AppenderPreservingEvents implements Appender {
        private final Appender inner;

        public AppenderPreservingEvents(Appender inner) {
            this.inner = inner;
        }

        @Override
        public void append(LogEvent event) {
            inner.append(event.toImmutable());
        }

        @Override
        public String getName() {
            return inner.getName();
        }

        @Override
        public Layout<? extends Serializable> getLayout() {
            return null;
        }

        @Override
        public boolean ignoreExceptions() {
            return false;
        }

        @Override
        public ErrorHandler getHandler() {
            return null;
        }

        @Override
        public void setHandler(ErrorHandler handler) {

        }

        @Override
        public State getState() {
            return null;
        }

        @Override
        public void initialize() {

        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public boolean isStarted() {
            return true;
        }

        @Override
        public boolean isStopped() {
            return false;
        }

        public Appender getInner() {
            return inner;
        }
    }
}
