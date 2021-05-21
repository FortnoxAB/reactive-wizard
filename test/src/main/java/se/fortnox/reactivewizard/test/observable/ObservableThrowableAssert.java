package se.fortnox.reactivewizard.test.observable;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssertAlternative;
import org.assertj.core.api.ThrowableTypeAssert;
import rx.Observable;

/**
 * Assertion class checking {@link Throwable} type for Observable.
 *
 * @param <T> type of throwable to be thrown.
 */
public class ObservableThrowableAssert<T extends Throwable> extends ThrowableTypeAssert<T> {

    /**
     * Default constructor.
     *
     * @param throwableType class representing the target (expected) exception.
     */
    public ObservableThrowableAssert(Class<? extends T> throwableType) {
        super(throwableType);
    }

    /**
     * Assert one onError signal with the given subclass of a Throwable as type
     * and allow to chain assertions on the thrown exception.
     *
     * @param errorEmittingObservable Observable emitting the error with exception of expected type
     * @return return a {@link ThrowableAssertAlternative}.
     */
    public ThrowableAssertAlternative<? extends T> isEmittedBy(Observable<?> errorEmittingObservable) {
        return Assertions.assertThatExceptionOfType(expectedThrowableType)
            .isThrownBy(() -> errorEmittingObservable.toBlocking().first());
    }
}
