package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class WaitingTestUtilsTest {

    @Test
    void shouldFailTestIfConditionIsNotMetWithinDefaultTime() {
        Error assertionError = null;
        try {
            WaitingTestUtils.assertConditionIsTrueWithinDefaultTime(() -> false);
        } catch (Error error) {
            assertionError = error;
        }

        assertThat(assertionError).isNotNull();
    }

    @Test
    void shouldFailTestIfConditionIsNotMetWithinDefaultTimeWithCustomMessage() {
        Error assertionError = null;
        String errorMessage = UUID.randomUUID().toString();
        try {
            WaitingTestUtils.assertConditionIsTrueWithinDefaultTime(() -> false, errorMessage);
        } catch (AssertionError error) {
            assertionError = error;
        }

        assertThat(assertionError).isNotNull();
        assertThat(assertionError.getMessage()).isEqualTo(errorMessage);
    }

    @Test
    void shouldFailTestIfConditionIsNotMetWithinTime() {
        Error assertionError = null;
        try {
            WaitingTestUtils.assertConditionIsTrueWithinTime(200, TimeUnit.MILLISECONDS, () -> false);
        } catch (Error error) {
            assertionError = error;
        }

        assertThat(assertionError).isNotNull();
    }


    @Test
    void shouldNotFailTestIfConditionIsMetWithinTime() {
        try {
            final BooleanSupplier mock = Mockito.mock(BooleanSupplier.class);
            when(mock.getAsBoolean()).thenReturn(false).thenReturn(false).thenReturn(true);
            WaitingTestUtils.assertConditionIsTrueWithinTime(1000, TimeUnit.MILLISECONDS, mock);
        } catch (Error assertionError) {
            Assertions.fail("This should not throw an assertionexception");
            assertionError.printStackTrace();
        }
    }

    @Test
    void shouldAcceptCustomErrorMessage() {
        final String errorMessage = UUID.randomUUID().toString();
        try {
            WaitingTestUtils.assertConditionIsTrueWithinTime(1000, TimeUnit.MILLISECONDS, () -> false, errorMessage);
        } catch (Error assertionError) {
            assertThat(assertionError.getMessage()).isEqualTo(errorMessage);
            assertionError.printStackTrace();
        }
    }

    @Test
    void shouldAllowBlocking() {
        WaitingTestUtils.assertConditionIsTrueWithinTime(1000, TimeUnit.MILLISECONDS, () ->
            Flux.just(true).blockLast());
    }
}
