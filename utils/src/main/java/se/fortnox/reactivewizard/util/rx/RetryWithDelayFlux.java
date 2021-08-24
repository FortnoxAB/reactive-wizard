package se.fortnox.reactivewizard.util.rx;

import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.function.Predicate;

public class RetryWithDelayFlux {

    public static Retry retry(int maxRetries, int inRetryDelayMillis) {
        return Retry.backoff(maxRetries, Duration.ofMillis(inRetryDelayMillis));
    }

    public static Retry retryWithExceptionFilter(int maxRetries, int inRetryDelayMillis, Predicate<? super Throwable> predicate) {
        return Retry
            .backoff(maxRetries, Duration.ofMillis(inRetryDelayMillis))
            .filter(predicate);
    }
}
