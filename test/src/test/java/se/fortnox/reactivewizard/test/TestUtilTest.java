package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.TestUtil.assertNestedException;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

public class TestUtilTest {
    @Test
    public void testMatchesAsExpected() {
        TestClass testClass = mock(TestClass.class);
        testClass.doNothing("expected");

        verify(testClass).doNothing(matches(string -> assertThat(string).isEqualTo("expected")));
    }

    @Test
    public void testMatchesFailure() {
        TestClass testClass = mock(TestClass.class);
        testClass.doNothing("unexpected");

        try {
            verify(testClass).doNothing(matches(string -> assertThat(string).isEqualTo("expected")));
            fail("Expected ComparisonFailure, but none was thrown");
        } catch (AssertionFailedError assertionFailedError) {
            assertThat(assertionFailedError.getActual().getStringRepresentation()).isEqualTo("""
                testClass.doNothing(
                    "unexpected"
                );
                """);
            assertThat(assertionFailedError.getExpected().getStringRepresentation()).isEqualTo("""
                testClass.doNothing(\n\s\s\s\s
                expected: "expected"
                 but was: "unexpected"
                );""");
            assertThat(assertionFailedError.getMessage()).isEqualTo("""

                Argument(s) are different! Wanted:
                testClass.doNothing(
                   \s
                expected: "expected"
                 but was: "unexpected"
                );
                -> at se.fortnox.reactivewizard.test.TestUtilTest$TestClass.doNothing(TestUtilTest.java:94)
                Actual invocations have different arguments:
                testClass.doNothing(
                    "unexpected"
                );
                -> at se.fortnox.reactivewizard.test.TestUtilTest.testMatchesFailure(TestUtilTest.java:28)
                   """);
        }
    }

    @Test
    public void testAssertTypeOfException() {
        TestException exception = new TestException();
        assertNestedException(exception, TestException.class);
    }

    @Test
    public void testAssertExceptionHandlingNull() {
        assertNestedException(null, TestException.class);
    }

    @Test
    public void testAssertTypeOfCause() {
        TestException exception = new TestException(new SQLException());
        assertNestedException(exception, SQLException.class);
    }

    @Test
    public void testExceptionOfWrongType() {
        TestException exception = new TestException(new SQLException());

        try {
            assertNestedException(exception, IOException.class);
            fail("Expected AssertionError, but none was thrown");
        } catch (AssertionError assertionError) {
            assertThat(assertionError).hasMessage("Expected exception of type java.io.IOException");
        }
    }

    class TestClass {
        void doNothing(String someString) {

        }
    }

    class TestException extends Throwable {
        TestException() {
            super();
        }

        TestException(Throwable cause) {
            super(cause);
        }
    }
}
