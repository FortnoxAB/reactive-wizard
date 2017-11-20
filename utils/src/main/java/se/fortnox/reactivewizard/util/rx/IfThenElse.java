package se.fortnox.reactivewizard.util.rx;

import rx.Observable;

public class IfThenElse<T> {
	private Observable<Boolean> ifValue;
	private Observable<T>       val;

	IfThenElse(Observable<Boolean> ifValue) {
		this.ifValue = ifValue;
	}

	public IfThenElse<T> then(Observable<T> val) {
		this.val = val;
		return this;
	}

	public Observable<T> elseThrow(Throwable e) {
		return ifValue.flatMap(exists -> {
			if (exists) {
				return val;
			} else {
				return Observable.error(e);
			}
		});
	}
}
