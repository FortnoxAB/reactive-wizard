package se.fortnox.reactivewizard.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

    @Test
    void shouldFailContainsExactlyAssertionWhenSpecifiedValuesAreExpectedButNotInOrder() {
        Observable<String> observableUnderTest = Observable.just("one", "two", "three");
        Assertions.assertThrows(AssertionError.class, () -> {
            ObservableAssertions.assertThat(observableUnderTest).containsExactly("one", "three", "two");
        });
    }

    @Test
    public void shouldFailContainsExactlyAssertionWhenValueIsUnexpected() {
        Observable<String> observableUnderTest = Observable.just("one");
        Assertions.assertThrows(AssertionError.class, () -> {
            ObservableAssertions.assertThat(observableUnderTest)
                .containsExactly("two");
        });
    }

    @Test
    public void shouldFailContainsExactlyAssertionWhenMoreThanOneValue() {
        Observable<String> observableUnderTest = Observable.just("one", "two");
        Assertions.assertThrows(AssertionError.class, () -> {
            ObservableAssertions.assertThat(observableUnderTest)
                .containsExactly("one");
        });
    }

    @Test
    public void shouldFailContainsExactlyAssertionWhenEmpty() {
        Observable<String> observableUnderTest = Observable.empty();
        Assertions.assertThrows(AssertionError.class, () -> {
            ObservableAssertions.assertThat(observableUnderTest)
                .containsExactly("one");
        });
    }

    @Test
    public void shouldReturnAssertionForOneValue() {
        Observable<String> observableUnderTest = Observable.just("one");
        ObservableAssertions.assertThat(observableUnderTest)
            .singleElement()
            .satisfies(value -> assertThat(value)
                .isEqualTo("one"));
    }

    @Test
    public void shouldFailToReturnAssertionForOneValueWhenMoreThanOne() {
        Observable<String> observableUnderTest = Observable.just("one", "two");
        Assertions.assertThrows(AssertionError.class, () -> {
            ObservableAssertions.assertThat(observableUnderTest)
                .singleElement()
                .satisfies(value -> assertThat(value)
                    .isEqualTo("one"));
        });
    }

    @Test
    public void shouldPassWhenEmptyObservableIsExpectedToBeEmpty() {
        Observable<String> observableUnderTest = Observable.empty();
        ObservableAssertions.assertThat(observableUnderTest)
            .isEmpty();
    }

    @Test
    public void shouldFailWhenNonEmptyObservableIsExpectedToBeEmpty() {
        Observable<String> observableUnderTest = Observable.just("one");
        Assertions.assertThrows(AssertionError.class, () -> {
            ObservableAssertions.assertThat(observableUnderTest)
                .isEmpty();
        });
    }
}
