package se.fortnox.reactivewizard.util;

import org.junit.Test;
import rx.Observable;
import rx.Subscriber;
import se.fortnox.reactivewizard.util.rx.RetryWithDelay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RetryWithDelayTest {

    private Observable<Integer> getRetryObservable() {
        RetryInterface retryInterface = mock(RetryInterface.class);
        when(retryInterface.retry()).thenThrow(new RuntimeException()).thenReturn(1);

        Observable<Integer> integerObservable = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                try {
                    subscriber.onNext(retryInterface.retry());
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });

        return integerObservable;
    }

    @Test
    public void shouldRetryWithPredicate() {
        Observable<Integer> retryObservable = getRetryObservable();

        Integer returnValue = retryObservable.retryWhen(new RetryWithDelay(1, 1, throwable -> true)).toBlocking().first();
        assertThat(returnValue).isEqualTo(1);
    }

    @Test
    public void shouldRetryWithoutPredicate() {
        Observable<Integer> retryObservable = getRetryObservable();

        Integer returnValue = retryObservable.retryWhen(new RetryWithDelay(1, 1)).toBlocking().first();
        assertThat(returnValue).isEqualTo(1);
    }

    @Test
    public void shouldNotRetryWithPredicate() {
        Observable<Integer> retryObservable = getRetryObservable();

        Exception exception = null;
        try {
            retryObservable.retryWhen(new RetryWithDelay(1, 1, throwable -> false)).toBlocking().first();
        } catch (Exception e) {
            exception = e;
        }
        assertThat(exception).isNotNull();
    }

    @Test
    public void shouldRetryForCertainError() {
        Observable<Integer> retryObservable = getRetryObservable();

        Integer returnValue = retryObservable.retryWhen(new RetryWithDelay(1, 1, RuntimeException.class)).toBlocking().first();
        assertThat(returnValue).isEqualTo(1);
    }

    @Test
    public void shouldNotRetryForCertainError() {
        Observable<Integer> retryObservable = getRetryObservable();

        Exception exception = null;
        try {
            retryObservable.retryWhen(new RetryWithDelay(1, 1, NumberFormatException.class)).toBlocking().first();
        } catch (Exception e) {
            exception = e;
        }
        assertThat(exception).isNotNull();
    }

    interface RetryInterface {
        int retry();
    }
}
