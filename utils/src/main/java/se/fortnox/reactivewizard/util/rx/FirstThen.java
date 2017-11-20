package se.fortnox.reactivewizard.util.rx;

import rx.Observable;
import rx.functions.Func0;

/**
 * Helper class used to chain sequential work in Rx, or omit foo variables, turning code like this:
 *
 * <pre>
 * {@code
 * doStuff().flatMap(foo->empty());
 * }
 * </pre>
 * Into this:
 *
 * <pre>
 * {@code
 * first(doStuff()).thenReturnEmpty();
 * }
 * </pre>
 *
 *
 */
public class FirstThen {

	private Observable<?> doFirst;

	FirstThen(Observable<?> doFirst) {
		this.doFirst = doFirst;
	}

	public <T> FirstThen then(Observable<T> thenReturn) {
		return new FirstThen(
				doFirst.lastOrDefault(null).flatMap(done -> thenReturn).map(done -> null));
	}

	public <T> FirstThen then(Func0<Observable<T>> thenFn) {
		return new FirstThen(
				doFirst.lastOrDefault(null).flatMap(done -> thenFn.call()).map(done -> null));
	}

	public <T> Observable<T> thenReturn(Func0<Observable<T>> thenFn) {
		return doFirst.lastOrDefault(null).flatMap(done -> thenFn.call());
	}

	public <T> Observable<T> thenReturn(T thenReturn) {
		return doFirst.lastOrDefault(null).map(done -> thenReturn);
	}

	public <T> Observable<T> thenReturn(Observable<T> thenReturn) {
		return doFirst.lastOrDefault(null).flatMap(done -> thenReturn);
	}

    /**
     * @param <T>
     * @return an empty observable, signalling success or error.
     */
    public <T> Observable<T> thenReturnEmpty() {
        return thenReturn(Observable.empty());
    }

}
