package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.String.format;
import static org.junit.platform.commons.support.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.support.ReflectionSupport.findFields;

/**
 * <p>Extension for JUnit Jupiter that provides support for LoggingVerifier lifecycle,
 * i.e. creating and destroying appenders
 * after each test case.</p>
 * <h2>Example</h2>
 *
 * <code class='java'>
 * {@literal @}ExtendWith(LoggingVerifierExtension.class)
 * class ExampleTestCase {
 * LoggingVerifier verifier = new LoggingVerifier(ClassThatLogsThings.class);
 * {@literal @}Test
 * void test() {
 * ...
 * }
 * }
 * </code>
 */
public class LoggingVerifierExtension implements BeforeEachCallback, AfterEachCallback {

    private static final Predicate<Field> LOGGING_VERIFIER_FIELD = field -> field.getType().equals(LoggingVerifier.class);

    @Override
    public void afterEach(ExtensionContext context) {
        getLoggingVerifiers(context)
            .forEach(LoggingVerifier::after);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        getLoggingVerifiers(context)
            .forEach(LoggingVerifier::before);
    }

    private static List<LoggingVerifier> getLoggingVerifiers(ExtensionContext context) {
        Object testInstance = context.getRequiredTestInstance();
        return findFields(testInstance.getClass(), LOGGING_VERIFIER_FIELD, TOP_DOWN).stream()
            .map(field -> {
                field.setAccessible(true);
                try {
                    return field.get(testInstance);
                } catch (IllegalAccessException e) {
                    throw new ExtensionConfigurationException(format("[%s] Could not gain read access to field", field.getName()), e);
                }
            })
            .map(LoggingVerifier.class::cast)
            .toList();
    }
}
