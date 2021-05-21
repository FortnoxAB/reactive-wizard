package se.fortnox.reactivewizard.test;

import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.test.observable.ObservableAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservableThrowableAssertTest {

    @Test
    public void shouldPassWhenExpectedExceptionIsThrown() {
        IllegalArgumentException exception = new IllegalArgumentException("Error");
        Observable<String> observableUnderTest = Observable.error(exception);
        ObservableAssertions.assertThatExceptionOfType(IllegalArgumentException.class)
            .isEmittedBy(observableUnderTest)
            .satisfies(illegalArgumentException ->
                assertThat(illegalArgumentException).isEqualTo(exception));
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExceptionOfUnexpectedTypeIsThrown() {
        IllegalArgumentException exception = new IllegalArgumentException("Error");
        Observable<String> observableUnderTest = Observable.error(exception);
        ObservableAssertions.assertThatExceptionOfType(IllegalStateException.class)
            .isEmittedBy(observableUnderTest);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenExceptionIsExpectedButNotThrown() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThatExceptionOfType(IllegalStateException.class)
            .isEmittedBy(observableUnderTest);
    }
}
