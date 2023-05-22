package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.Test;
import rx.Observable;
import se.fortnox.reactivewizard.test.observable.ObservableAssertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObservableThrowableAssertTest {

    @Test
    void shouldPassWhenExpectedExceptionIsThrown() {
        IllegalArgumentException exception = new IllegalArgumentException("Error");
        Observable<String> observableUnderTest = Observable.error(exception);
        ObservableAssertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isEmittedBy(observableUnderTest)
            .satisfies(illegalArgumentException ->
                assertThat(illegalArgumentException).isEqualTo(exception));
    }

    @Test
    void shouldFailWhenExceptionOfUnexpectedTypeIsThrown() {
        assertThrows(AssertionError.class, () -> {
            IllegalArgumentException exception = new IllegalArgumentException("Error");
            Observable<String> observableUnderTest = Observable.error(exception);
            ObservableAssertions.assertThatExceptionOfType(IllegalStateException.class)
                .isEmittedBy(observableUnderTest);
        });
    }

    @Test
    void shouldFailWhenExceptionIsExpectedButNotThrown() {
        assertThrows(AssertionError.class, () -> {
            Observable<String> observableUnderTest = Observable.just("one");
            ObservableAssertions.assertThatExceptionOfType(IllegalStateException.class)
                .isEmittedBy(observableUnderTest);
        });
    }
}
