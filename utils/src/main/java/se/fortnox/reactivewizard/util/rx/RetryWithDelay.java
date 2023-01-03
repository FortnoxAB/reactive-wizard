package se.fortnox.reactivewizard.util.rx;

import reactor.util.retry.Retry;

import java.util.function.Predicate;

import static java.time.Duration.ofMillis;
import static reactor.util.retry.Retry.fixedDelay;

public class RetryWithDelay {

    private RetryWithDelay() {
    }

    /**
     * A wrapper method around RetrySpec.
     * Retries if exceptionType is null or matches the propagated exception,
     * otherwise it continues to propagate the error.
     * @param maxRetries maximum number of retries
     * @param retryDelayMillis retry delay in milliseconds
     * @param exceptionType exception type to check against
     * @return Retry strategy that retries only if exceptionType is null or matches the propagated exception.
     */
    public static Retry retryWithDelay(int maxRetries, int retryDelayMillis, final Class<? extends Throwable> exceptionType) {
        return retryWithDelay(
            maxRetries,
            retryDelayMillis,
            throwable -> exceptionType == null || exceptionType.isAssignableFrom(throwable.getClass())
        );
    }

    /**
     * A wrapper method around RetrySpec.
     * Retries if prediate returns true given the propagated exception.
     * @param maxRetries maximum number of retries
     * @param retryDelayMillis retry delay in millseconds
     * @param predicate predicate that decides to retry or not
     * @return Retry strategy that retries only if predicate returns true given the propagated exception.
     */
    public static Retry retryWithDelay(int maxRetries, int retryDelayMillis, final Predicate<? super Throwable> predicate) {
        return fixedDelay(maxRetries, ofMillis(retryDelayMillis)).filter(predicate);
    }

}
