package se.fortnox.reactivewizard.db;

import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;
import reactor.util.annotation.NonNull;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

import static java.util.Objects.nonNull;

/**
 * This class is a wrapper class around FluxSink.
 *
 * Since DAO methods should support both Flux and
 * Mono return types, there needs to be a component
 * that is responsible for resolving the issues
 * caused from using FluxSink when the DAO method
 * returns a Mono.
 *
 * @param <T> type parameter for the database result
 */
class DaoFluxSink<T> implements FluxSink<T> {
    private final AtomicLong    numNextSignals;
    private final FluxSink<T>   actual;
    private final boolean       isMono;
    private final String        methodName;

    private T firstElement;

    public DaoFluxSink(FluxSink<T> actual, boolean isMono, String methodName) {
        this.actual = actual;
        this.isMono = isMono;
        this.methodName = methodName;
        this.numNextSignals = new AtomicLong(1);
    }

    @Override
    @NonNull
    public FluxSink<T> next(@NonNull T next) {
        if (isMono) {
            long numSignals = numNextSignals.getAndIncrement();
            if (numSignals > 1) {
                actual.error(new RuntimeException(String.format(
                    "%s returning a Mono received more than one result from the database", methodName)));
                return actual;
            }
            firstElement = next;
            return actual;
        } else {
            return actual.next(next);
        }
    }

    @Override
    public void complete() {
        if (isMono && nonNull(firstElement)) {
            actual.next(firstElement);
        }
        actual.complete();
    }

    @Override
    public void error(@NonNull Throwable exception) {
        if (isMono && numNextSignals.get() > 1) {
            complete();
        } else {
            actual.error(exception);
        }
    }

    @Override
    @NonNull
    public Context currentContext() {
        return actual.currentContext();
    }

    @Override
    @NonNull
    public ContextView contextView() {
        return actual.contextView();
    }

    @Override
    public long requestedFromDownstream() {
        return actual.requestedFromDownstream();
    }

    @Override
    public boolean isCancelled() {
        return actual.isCancelled();
    }

    @Override
    @NonNull
    public FluxSink<T> onRequest(@NonNull LongConsumer consumer) {
        return actual.onRequest(consumer);
    }

    @Override
    @NonNull
    public FluxSink<T> onCancel(@NonNull Disposable disposable) {
        return actual.onCancel(disposable);
    }

    @Override
    @NonNull
    public FluxSink<T> onDispose(@NonNull Disposable disposable) {
        return actual.onDispose(disposable);
    }
}
