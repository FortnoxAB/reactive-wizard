package se.fortnox.reactivewizard.util.rx;

import rx.Observable;

public class IfThenElse<T> {
    private Observable<Boolean> ifValue;
    private Observable<T>       thenValue;

    IfThenElse(Observable<Boolean> ifValue) {
        this.ifValue = ifValue;
    }

    public IfThenElse<T> then(Observable<T> thenValue) {
        this.thenValue = thenValue;
        return this;
    }

    public Observable<T> elseThrow(Throwable throwable) {
        return ifValue.flatMap(exists -> {
            if (exists) {
                return thenValue;
            } else {
                return Observable.error(throwable);
            }
        });
    }
}
