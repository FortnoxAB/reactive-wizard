package se.fortnox.reactivewizard.test.publisher;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.assertj.core.api.ThrowableTypeAssert;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Assertion class checking {@link Throwable} type for Publisher.
 *
 * @param <T> type of throwable to be thrown.
 */
public class PublisherThrowableAssert<T extends Throwable> extends ThrowableTypeAssert<T> {

    /**
     * Default constructor.
     *
     * @param throwableType class representing the target (expected) exception.
     */
    public PublisherThrowableAssert(Class<? extends T> throwableType) {
        super(throwableType);
    }

    /**
     * Assert one onError signal with the given subclass of a Throwable as type
     * and allow to chain assertions on the thrown exception.
     *
     * @param errorEmittingPublisher Publisher emitting the error with exception of expected type
     * @return return a {@link ThrowableAssertAlternative}.
     */
    public ThrowableAssertAlternative<? extends T> isEmittedBy(Publisher<?> errorEmittingPublisher) {
        return Assertions.assertThatExceptionOfType(expectedThrowableType)
            .isThrownBy(() -> Flux.from(errorEmittingPublisher).blockLast());
    }
}
