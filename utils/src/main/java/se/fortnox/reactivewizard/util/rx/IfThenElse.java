package se.fortnox.reactivewizard.util.rx;

import rx.Observable;

/**
 * Utility for creating simple conditional workflows using Observables.
 */
public class IfThenElse<T> {
    private Observable<Boolean> ifValue;
    private Observable<T>       thenValue;

    IfThenElse(Observable<Boolean> ifValue) {
        this.ifValue = ifValue;
    }

    /**
     * Executes when the boolean Observable is true.
     *
     * @param thenValue The observable to execute
     * @return this class
     */
    public IfThenElse<T> then(Observable<T> thenValue) {
        this.thenValue = thenValue;
        return this;
    }

    /**
     * Thrown when the boolean Observable is false.
     *
     * @param throwable The exception to throw
     * @return then Observable or error Observable
     */
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
