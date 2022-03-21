package se.fortnox.reactivewizard.test;

import org.assertj.core.api.AbstractThrowableAssert;
import org.mockito.ArgumentMatcher;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * Utility functions for building tests.
 */
public final class TestUtil {
    private TestUtil() {

    }

    /**
     * Wraps a lambda expression which does assertions on your object.
     * <p>
     * Example:
     * </p>
     * <pre>
     * verify(assignmentDao).createAssignment(matches((assignment -&gt; {
     *     assertThat(assignment.getTitle()).isEqualTo(&quot;Assignment 1&quot;);
     *     assertThat(assignment.getClientId()).isEqualTo(&quot;501780&quot;);
     * })));
     * </pre>
     *
     * @param asserter a lambda expression doing assertions
     * @param <T>      is the type of consumer value
     * @return null
     */
    public static <T> T matches(Consumer<T> asserter) {
        return argThat(new ArgumentMatcher<T>() {
            Error error;

            @SuppressWarnings("unchecked")
            @Override
            public boolean matches(Object argument) {
                try {
                    asserter.accept((T) argument);
                } catch (Error t) {
                    error = t;
                    return false;
                }
                return true;
            }

            @Override
            public String toString() {
                if (error != null) {
                    return error.getMessage();
                }
                return "";
            }
        });
    }

    /**
     * Assert the type of an exception.
     *
     * @param <T>       The type of the expected exception
     * @param throwable The exception to apply assertion on
     * @param type      Expected type of exception
     * @return The assertion for further assertion chaining
     */
    public static <T extends Throwable> AbstractThrowableAssert assertNestedException(Throwable throwable, Class<T> type) {
        while (throwable != null && !type.isAssignableFrom(throwable.getClass())) {
            Throwable cause = throwable.getCause();
            if (cause == throwable || cause == null) {
                fail("Expected exception of type " + type.getCanonicalName());
            }
            throwable = cause;
        }
        return assertThat(throwable);
    }
}
