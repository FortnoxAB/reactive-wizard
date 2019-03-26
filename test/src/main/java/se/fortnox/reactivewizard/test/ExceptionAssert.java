package se.fortnox.reactivewizard.test;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Objects;

import static org.assertj.core.api.Assertions.fail;

public class ExceptionAssert extends AbstractAssert<ExceptionAssert, Throwable> {
    private Class<?>

    public ExceptionAssert(Throwable actual) {
        super(actual, ExceptionAssert.class);
    }

    public static ExceptionAssert assertThat(Throwable actual) {
        return new ExceptionAssert(actual);
    }

    public ExceptionAssert isInstanceOf(Class<?> expected) {
        isNotNull();

        Throwable throwable = actual;
        while (!expected.isAssignableFrom(throwable.getClass())) {
            Throwable cause = throwable.getCause();
            if (cause == throwable || cause == null) {
                failWithMessage("Expected exception of type %s", expected.getCanonicalName());
            }
            throwable = cause;
        }

        return this;
    }

    public ExceptionAssert hasMessage(String message) {
        isNotNull();
        /*
        // check condition
        if (!actual.getName().equals(name)) {
            failWithMessage("Expected character's name to be <%s> but was <%s>", name, actual.getName());
        }
        */

        // return the current assertion for method chaining
        return this;
    }
}
