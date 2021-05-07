package se.fortnox.reactivewizard.test;

import org.junit.Test;
import org.mockito.exceptions.verification.junit.ArgumentsAreDifferent;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.TestUtil.*;

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
        } catch (ArgumentsAreDifferent comparisonFailure) {
            assertThat(comparisonFailure.getActual()).isEqualTo("testClass.doNothing(\n" +
                    "    \"unexpected\"\n" +
                    ");\n");
            assertThat(comparisonFailure.getExpected()).isEqualTo("testClass.doNothing(\n" +
                "    expected:<\"[]expected\"> but was:<\"[un]expected\">\n" +
                ");");
            assertThat(comparisonFailure.getMessage()).isEqualTo("\n" +
                    "Argument(s) are different! Wanted:\n" +
                    "testClass.doNothing(\n" +
                    "    expected:<\"[]expected\"> but was:<\"[un]expected\">\n" +
                    ");\n" +
                    "-> at se.fortnox.reactivewizard.test.TestUtilTest.testMatchesFailure(TestUtilTest.java:32)\n" +
                    "Actual invocations have different arguments:\n" +
                    "testClass.doNothing(\n" +
                    "    \"unexpected\"\n" +
                    ");\n" +
                    "-> at se.fortnox.reactivewizard.test.TestUtilTest.testMatchesFailure(TestUtilTest.java:29)\n");
        }
    }

    @Test
    public void testAssertTypeOfExceptionFest() {
        TestException exception = new TestException();
        assertException(exception, TestException.class);
    }

    @Test
    public void testAssertExceptionHandlingNullFest() {
        assertException(null, TestException.class);
    }

    @Test
    public void testAssertTypeOfCauseFest() {
        TestException exception = new TestException(new SQLException());
        assertException(exception, SQLException.class);
    }

    @Test
    public void testExceptionOfWrongTypeFest() {
        TestException exception = new TestException(new SQLException());

        try {
            assertException(exception, IOException.class);
            fail("Expected AssertionError, but none was thrown");
        } catch (AssertionError assertionError) {
            assertThat(assertionError).hasMessage("Expected exception of type java.io.IOException");
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
