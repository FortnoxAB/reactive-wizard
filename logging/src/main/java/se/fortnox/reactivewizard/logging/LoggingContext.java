package se.fortnox.reactivewizard.logging;

import org.slf4j.MDC;
import rx.Observable;
import rx.Observer;

import java.util.Map;

/**
 * Handle MDC context for Observables.
 */
public class LoggingContext {

    public static void reset() {
        MDC.clear();
    }

    /**
     * Restore logging context after the Observable completes.
     *
     * @param toObservable The Observable to keep context around
     * @return The Observable stream
     */
    public static <T> Observable<T> transfer(Observable<T> toObservable) {
        final Map context = MDC.getCopyOfContextMap();
        return toObservable.doOnEach(new Observer<T>() {
            @Override
            public void onCompleted() {
                restoreContext();
            }

            @Override
            public void onError(Throwable throwable) {
                restoreContext();
            }

            @Override
            public void onNext(T observable) {
                restoreContext();
            }

            private void restoreContext() {
                if (context != null) {
                    MDC.setContextMap(context);
                }
            }
        });
    }
}
