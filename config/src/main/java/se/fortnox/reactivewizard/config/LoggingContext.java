package se.fortnox.reactivewizard.config;

import org.slf4j.MDC;
import rx.Observable;
import rx.Observer;

import java.util.Map;

public class LoggingContext {

    public static void reset() {
        MDC.clear();
    }

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
