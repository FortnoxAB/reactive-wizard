package se.fortnox.reactivewizard.test.observable;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import rx.Observable;

import java.util.List;

/**
 * Assertion for an Observable.
 *
 * @param <T> the type of the items emitted by the Observable under test
 */
public class ObservableAssert<T> extends AbstractAssert<ObservableAssert<T>, Observable<T>> {

    /**
     * Default constructor.
     *
     * @param actual the Observable under test
     */
    public ObservableAssert(Observable<T> actual) {
        super(actual, ObservableAssert.class);
        isNotNull();
    }

    /**
     * Verifies that the Observable receives a single event with specified value and no errors.
     *
     * @param expected expected value
     * @return a new assertion object whose object under test is the expected value
     */
    public ObjectAssert<T> hasValue(T expected) {
        List<T> values = actual.test().awaitTerminalEvent().assertNoErrors().getOnNextEvents();
        Assertions.assertThat(values)
            .describedAs("Expected one value, but got %s.", values)
            .hasSize(1);
        return Assertions.assertThat(values.get(0))
            .isEqualTo(expected);
    }

    /**
     * Verifies that the Observable receives a single event and no errors.
     *
     * @return a new assertion object whose object under test is the received event
     */
    public ObjectAssert<T> hasOneValue() {
        List<T> values = actual.test().awaitTerminalEvent().assertNoErrors().getOnNextEvents();
        Assertions.assertThat(values)
            .describedAs("Expected one value, but got %s.", values)
            .hasSize(1);
        return Assertions.assertThat(values.get(0));
    }

    /**
     * Verifies that the Observable emits no values.
     */
    public void isEmpty() {
        actual.test().awaitTerminalEvent().assertNoValues();
    }
}
