package se.fortnox.reactivewizard;

import se.fortnox.reactivewizard.util.rx.RetryWithDelay;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RetryWithDelayTest {

	interface RetryInterface {
		int retry();
	}

	private Observable<Integer> getRetryObservable() {
		RetryInterface retryInterface = mock(RetryInterface.class);
		when(retryInterface.retry()).thenThrow(new RuntimeException()).thenReturn(1);

		Observable<Integer> obs = Observable.create(new Observable.OnSubscribe<Integer>() {
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

		return obs;
	}

	@Test
	public void shouldRetryWithPredicate() {
		Observable<Integer> obs = getRetryObservable();

		Integer i = obs.retryWhen(new RetryWithDelay(1, 1, p -> true)).toBlocking().first();
		assertThat(i).isEqualTo(1);
	}

	@Test
	public void shouldNotRetryWithPredicate() {
		Observable<Integer> obs = getRetryObservable();

		Exception e = null;
		try {
			obs.retryWhen(new RetryWithDelay(1, 1, p -> false)).toBlocking().first();
		} catch (Exception ex) {
			e = ex;
		}
		assertThat(e).isNotNull();
	}

	@Test
	public void shouldRetryForCertainError() {
		Observable<Integer> obs = getRetryObservable();

		Integer i = obs.retryWhen(new RetryWithDelay(1, 1, RuntimeException.class)).toBlocking().first();
		assertThat(i).isEqualTo(1);
	}

	@Test
	public void shouldNotRetryForCertainError() {
		Observable<Integer> obs = getRetryObservable();

		Exception e = null;
		try {
			obs.retryWhen(new RetryWithDelay(1, 1, NumberFormatException.class)).toBlocking().first();
		} catch (Exception ex) {
			e = ex;
		}
		assertThat(e).isNotNull();
	}
}
