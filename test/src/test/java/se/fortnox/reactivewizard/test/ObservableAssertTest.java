package se.fortnox.reactivewizard.test;

import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.test.observable.ObservableAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class ObservableAssertTest {

    @Test
    public void shouldPassContainsExactlyAssertionWhenSpecifiedValueIsExpected() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .containsExactly("one");
    }

    @Test
    public void shouldPassContainsExactlyAssertionWhenSpecifiedValuesAreExpectedAndInOrder() {
        Observable<String> observableUnderTest = Observable.just("one", "two", "three");
        ObservableAssertions.assertThat(observableUnderTest)
            .containsExactly("one", "two", "three");
    }

    @Test(expected = AssertionError.class)
    public void shoulFailContainsExactlyAssertionWhenSpecifiedValuesAreExpectedButNotInOrder() {
        Observable<String> observableUnderTest = Observable.just("one", "two", "three");
        ObservableAssertions.assertThat(observableUnderTest)
            .containsExactly("one", "three", "two");
    }

    @Test(expected = AssertionError.class)
    public void shouldFailContainsExactlyAssertionWhenValueIsUnexpected() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .containsExactly("two");
    }

    @Test(expected = AssertionError.class)
    public void shouldFailContainsExactlyAssertionWhenMoreThanOneValue() {
        Observable<String> observableUnderTest = Observable.just("one", "two");
        ObservableAssertions.assertThat(observableUnderTest)
            .containsExactly("one");
    }

    @Test(expected = AssertionError.class)
    public void shouldFailContainsExactlyAssertionWhenEmpty() {
        Observable<String> observableUnderTest = Observable.empty();
        ObservableAssertions.assertThat(observableUnderTest)
            .containsExactly("one");
    }

    @Test
    public void shouldReturnAssertionForOneValue() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .singleElement()
            .satisfies(value -> assertThat(value)
                .isEqualTo("one"));
    }

    @Test(expected = AssertionError.class)
    public void shouldFailToReturnAssertionForOneValueWhenMoreThanOne() {
        Observable<String> observableUnderTest = Observable.just("one", "two");
        ObservableAssertions.assertThat(observableUnderTest)
            .singleElement()
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
