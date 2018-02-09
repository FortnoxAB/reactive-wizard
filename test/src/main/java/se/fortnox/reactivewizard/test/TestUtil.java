package se.fortnox.reactivewizard.test;

import org.fest.assertions.ThrowableAssert;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.util.function.Consumer;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

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
     * <p>
     * <pre>
     * verify(assignmentDAO).createAssignment(matches((ass -&gt; {
     *  assertThat(ass.getTitle()).isEqualTo(&quot;Ass 1&quot;);
     *  assertThat(ass.getClientId()).isEqualTo(&quot;501780&quot;);
     * })));
     * </pre>
     *
     * @param asserter a lambda expression doing assertions
     * @return
     */
    public static <T> T matches(Consumer<T> asserter) {
        return Mockito.argThat(new ArgumentMatcher<T>() {
            Error error;

            @SuppressWarnings("unchecked")
            @Override
            public boolean matches(Object argument) {
                try {
                    asserter.accept((T)argument);
                } catch (Error t) {
                    error = t;
                    return false;
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                if (error != null) {
                    throw error;
                }
            }
        });
    }

    /**
     * Assert the type of an exception.
     *
     * @param throwable The exception to apply assertion on
     * @param type Expected type of exception
     * @return The assertion for further assertion chaining
     */
    public static <T extends Throwable> ThrowableAssert assertException(Throwable throwable, Class<T> type) {
        while (!type.isAssignableFrom(throwable.getClass())) {
            Throwable cause = throwable.getCause();
            if (cause == throwable) {
                fail("wrong type of exception");
            }
            throwable = cause;
        }
        return assertThat(throwable);
    }

}
