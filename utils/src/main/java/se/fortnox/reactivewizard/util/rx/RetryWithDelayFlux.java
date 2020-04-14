package se.fortnox.reactivewizard.util.rx;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryWithDelayFlux implements Function<Flux<Throwable>, Flux<?>> {

    private final int                          maxRetries;
    private final int                          retryDelayMillis;
    private final Predicate<? super Throwable> predicate;
    private       int                          retryCount;

    public RetryWithDelayFlux(int maxRetries, int retryDelayMillis, final Class<? extends Throwable> exceptionType) {
        this(maxRetries, retryDelayMillis, throwable -> exceptionType == null || exceptionType.isAssignableFrom(throwable.getClass()));
    }

    public RetryWithDelayFlux(int maxRetries, int retryDelayMillis, final Predicate<? super Throwable> predicate) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.predicate = predicate;
        this.retryCount = 0;
    }

    public RetryWithDelayFlux(int maxRetries, int retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
        this.predicate = null;
        this.retryCount = 0;
    }

    @Override
    public Flux<?> apply(Flux<Throwable> attempts) {
        return attempts.flatMap(throwable -> {
            if (++retryCount <= maxRetries && (predicate == null || predicate.test(throwable))) {
                int delay = retryDelayMillis * retryCount;
                return Flux.just(0).delaySubscription(Duration.ofMillis(delay));
            }

            // Max retries hit. Just pass the error along.
            return Flux.error(throwable);
        });
    }
}
