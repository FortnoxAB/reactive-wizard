package se.fortnox.reactivewizard.test;

import org.junit.ComparisonFailure;
import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.test.observable.ObservableAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservableAssertTest {

    @Test
    public void shouldPassHasValueAssertionWhenSpecifiedValueIsExpected() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .hasValue("one");
    }

    @Test(expected = ComparisonFailure.class)
    public void shouldFailHasValueAssertionWhenValueIsUnexpected() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .hasValue("two");
    }

    @Test(expected = AssertionError.class)
    public void shouldFailHasValueAssertionWhenMoreThanOneValue() {
        Observable<String> observableUnderTest = Observable.just("one", "two");
        ObservableAssertions.assertThat(observableUnderTest)
            .hasValue("one");
    }

    @Test(expected = AssertionError.class)
    public void shouldFailHasValueAssertionWhenEmpty() {
        Observable<String> observableUnderTest = Observable.empty();
        ObservableAssertions.assertThat(observableUnderTest)
            .hasValue("one");
    }

    @Test
    public void shouldReturnAssertionForOneValue() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .hasOneValue()
            .satisfies(value -> assertThat(value)
                .isEqualTo("one"));
    }

    @Test(expected = AssertionError.class)
    public void shouldFailToReturnAssertionForOneValueWhenMoreThanOne() {
        Observable<String> observableUnderTest = Observable.just("one", "two");
        ObservableAssertions.assertThat(observableUnderTest)
            .hasOneValue()
            .satisfies(value -> assertThat(value)
                .isEqualTo("one"));
    }

    @Test
    public void shouldPassWhenEmptyObservableIsExpectedToBeEmpty() {
        Observable<String> observableUnderTest = Observable.empty();
        ObservableAssertions.assertThat(observableUnderTest)
            .isEmpty();
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenNonEmptyObservableIsExpectedToBeEmpty() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .isEmpty();
    }
}
