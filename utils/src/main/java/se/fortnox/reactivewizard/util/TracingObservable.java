package se.fortnox.reactivewizard.util;

import rx.Observable;

public class TracingObservable {

	public static <T> Observable<T> trace(Observable<T> obs) {
		try {
			throw new Exception("Created at");
		} catch (Exception e) {
			return obs.onErrorResumeNext(exc -> Observable.error(e
					.initCause(exc)));
		}
	}
}
