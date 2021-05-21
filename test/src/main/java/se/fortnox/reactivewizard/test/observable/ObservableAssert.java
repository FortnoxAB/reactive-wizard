package se.fortnox.reactivewizard.test.observable;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import rx.Observable;

import java.util.Arrays;
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
     * Verifies that the Observable receives exactly the values given and nothing else <b>in the same order</b>.
     *
     * @param expected expected value
     * @return a new assertion object whose object under test is a list of the expected values
     */
    public ListAssert<T> containsExactly(T... expected) {
        List<T> values = actual.test().awaitTerminalEvent().assertNoErrors().getOnNextEvents();
        return Assertions.assertThat(values)
            .containsExactlyElementsOf(Arrays.asList(expected));
    }

    /**
     * Verifies that the Observable receives a single event and no errors.
     *
     * @return a new assertion object whose object under test is the received event
     */
    public ObjectAssert<T> singleElement() {
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
