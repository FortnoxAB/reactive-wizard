package se.fortnox.reactivewizard.util.rx;

import rx.Observable;
import rx.functions.Func1;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class RetryWithDelay implements Func1<Observable<? extends Throwable>, Observable<?>> {

    private final int                          maxRetries;
    private final int                          retryDelayMillis;
    private final Predicate<? super Throwable> predicate;
    private       int                          retryCount;

    public RetryWithDelay(int maxRetries, int retryDelayMillis, final Class<? extends Throwable> exceptionType) {
        this(maxRetries, retryDelayMillis, throwable -> {
            return exceptionType == null || exceptionType.isAssignableFrom(throwable.getClass());
        });
    }

    public RetryWithDelay(int maxRetries, int retryDelayMillis, final Predicate<? super Throwable> predicate) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.predicate = predicate;
        this.retryCount = 0;
    }

    public RetryWithDelay(int maxRetries, int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.predicate = null;
        this.retryCount = 0;
    }

    @Override
    public Observable<?> call(Observable<? extends Throwable> attempts) {
        return attempts.flatMap(throwable -> {
            if (++retryCount <= maxRetries && (predicate == null || predicate.test(throwable))) {
                int delay = retryDelayMillis * retryCount;
                return Observable.timer(delay, TimeUnit.MILLISECONDS);
            }

            // Max retries hit. Just pass the error along.
            return Observable.error(throwable);
        });
    }
}
