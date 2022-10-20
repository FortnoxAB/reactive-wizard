package se.fortnox.reactivewizard.test.publisher;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

/**
 * Assertion for a Publisher.
 *
 * @param <T> the type of the items emitted by the Publisher under test
 */
public class PublisherAssert<T> extends AbstractAssert<PublisherAssert<T>, Publisher<T>> {

    /**
     * Default constructor.
     *
     * @param actual the Publisher under test
     */
    public PublisherAssert(Publisher<T> actual) {
        super(actual, PublisherAssert.class);
        isNotNull();
    }

    /**
     * Verifies that the Publisher receives exactly the values given and nothing else <b>in the same order</b>.
     *
     * @param expected expected value
     * @return a new assertion object whose object under test is a list of the expected values
     */
    public ListAssert<T> containsExactly(T... expected) {
        StepVerifier.create(actual).expectNext(expected).verifyComplete();
        return Assertions.assertThatList(List.of(expected));
    }

    /**
     * Verifies that the Publisher receives a single event and no errors.
     *
     * @return a new assertion object whose object under test is the received event
     */
    public ObjectAssert<T> singleElement() {
        List<T> values = Flux.from(actual).collectList().block();
        Assertions.assertThat(values)
            .describedAs("Expected one value, but got %s.", values)
            .hasSize(1);
        return Assertions.assertThat(values.get(0));
    }

    /**
     * Verifies that the Publisher emits no values.
     */
    public void isEmpty() {
        StepVerifier.create(actual).verifyComplete();
    }


}
