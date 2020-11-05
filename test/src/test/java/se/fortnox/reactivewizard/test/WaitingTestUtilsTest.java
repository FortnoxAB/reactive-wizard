package se.fortnox.reactivewizard.test;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class WaitingTestUtilsTest {

    @Test
    public void shouldFailTestIfConditionIsNotMetWithinDefaultTime() {
        Error assertionError = null;
        try {
            WaitingTestUtils.assertConditionIsTrueWithin5Seconds(() -> false);
        } catch (Error error) {
            assertionError = error;
        }

        assertThat(assertionError).isNotNull();
    }

    @Test
    public void shouldFailTestIfConditionIsNotMetWithinDefaultTimeWithCustomMessage() {
        Error assertionError = null;
        String errorMessage = UUID.randomUUID().toString();
        try {
            WaitingTestUtils.assertConditionIsTrueWithin5Seconds(() -> false, errorMessage);
        } catch (AssertionError error) {
            assertionError = error;
        }

        assertThat(assertionError).isNotNull();
        assertThat(assertionError.getMessage()).isEqualTo(errorMessage);
    }

    @Test
    public void shouldFailTestIfConditionIsNotMetWithinTime() {
        Error assertionError = null;
        try {
            WaitingTestUtils.assertConditionIsTrueWithinTime(3000, TimeUnit.MILLISECONDS, () -> false);
        } catch (Error error) {
            assertionError = error;
        }

        assertThat(assertionError).isNotNull();
    }


    @Test
    public void shouldNotFailTestIfConditionIsMetWithinTime() {
        try {
            final BooleanSupplier mock = Mockito.mock(BooleanSupplier.class);
            when(mock.getAsBoolean()).thenReturn(false).thenReturn(false).thenReturn(true);
            WaitingTestUtils.assertConditionIsTrueWithinTime(1000, TimeUnit.MILLISECONDS, mock);
        } catch (Error assertionError) {
            Assert.fail("This should not throw an assertionexception");
            assertionError.printStackTrace();
        }
    }

    @Test
    public void shouldAcceptCustomErrorMessage() {
        final String errorMessage = UUID.randomUUID().toString();
        try {
            WaitingTestUtils.assertConditionIsTrueWithinTime(1000, TimeUnit.MILLISECONDS, () -> false, errorMessage);
        } catch (Error assertionError) {
            assertThat(assertionError.getMessage()).isEqualTo(errorMessage);
            assertionError.printStackTrace();
        }
    }
}
