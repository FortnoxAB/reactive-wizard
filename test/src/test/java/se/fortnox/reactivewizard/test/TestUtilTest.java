package se.fortnox.reactivewizard.test;

import org.junit.ComparisonFailure;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.TestUtil.assertException;
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
        } catch (ComparisonFailure comparisonFailure) {
            assertThat(comparisonFailure.getActual()).isEqualTo("\"unexpected\"");
            assertThat(comparisonFailure.getExpected()).isEqualTo("\"expected\"");
            assertThat(comparisonFailure.getMessage()).isEqualTo("expected:<\"[]expected\"> but was:<\"[un]expected\">");
        }
    }

    @Test
    public void testAssertTypeOfException() {
        TestException exception = new TestException();

        assertException(exception, TestException.class);
    }

    @Test
    public void testAssertTypeOfCause() {
        TestException exception = new TestException(new SQLException());

        assertException(exception, SQLException.class);
    }

    @Test
    public void testExceptionOfWrongType() {
        TestException exception = new TestException(new SQLException());

        try {
            assertException(exception, IOException.class);
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
