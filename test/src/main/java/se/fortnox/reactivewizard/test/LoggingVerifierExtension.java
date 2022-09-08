package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;

import java.lang.reflect.Field;
import java.util.List;

import static java.lang.String.format;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields;
import static org.junit.platform.commons.support.ModifierSupport.isStatic;

/**
 * <p>Extension for JUnit Jupiter that provides support for injecting instances of LoggingVerifier and providing lifecycle support,
 * i.e. creating and destroying appenders
 * after each test case.</p>
 * <p>Fields that are to be injected must be annotated with @LoggingVerifierFor(ClassToVerify.class).</p>
 * <h2>Example</h2>
 *
 * <code class='java'>
 * {@literal @}ExtendWith(LoggingVerifierExtension.class)
 * class ExampleTestCase {
 *    {@literal @}LoggingVerifierFor(ClassThatLogsThings.class)
 *    LoggingVerifier verifier;
 *    {@literal @}Test
 *    void test() {
 *       ...
 *    }
 * }
 * </code>
 */
public class LoggingVerifierExtension implements TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback {

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        List<Field> loggingVerifierFields = findAnnotatedFields(testInstance.getClass(), LoggingVerifierFor.class);
        loggingVerifierFields.forEach(field -> {
            checkCorrectClass(field);
            checkNotStatic(field);
            LoggingVerifierFor annotation = field.getAnnotationsByType(LoggingVerifierFor.class)[0];
            Class<?> classToVerify = annotation.value();
            LoggingVerifier loggingVerifier = new LoggingVerifier(classToVerify);
            field.setAccessible(true);
            try {
                field.set(testInstance, loggingVerifier);
            } catch (IllegalAccessException e) {
                throw new ExtensionConfigurationException(format("[%s] Could not gain write access to field", field.getName()), e);
            }
        });
    }

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
        return findAnnotatedFields(testInstance.getClass(), LoggingVerifierFor.class).stream()
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

    private static void checkNotStatic(Field field) {
        if (isStatic(field)) {
            throw new ExtensionConfigurationException("Fields annotated with @LoggingVerifierFor must not be static");
        }
    }

    private static void checkCorrectClass(Field field) {
        if (!field.getType().equals(LoggingVerifier.class)) {
            throw new ExtensionConfigurationException("Fields annotated with @LoggingVerifierFor must be of type LoggingVerifier");
        }
    }
}
