package se.fortnox.reactivewizard.util.rx;

import rx.Observable;
import rx.functions.Func0;

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

}
