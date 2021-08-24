package se.fortnox.reactivewizard.util.rx;

import reactor.core.publisher.Flux;

import java.util.function.Supplier;

/**
 * Helper class used to chain sequential work in Rx, or omit foo variables. Turns code like this:
 * <p>
 * <pre>
 * {@code
 * doStuff().flatMap(foo->empty());
 * }
 * </pre>
 * Into this:
 * <p>
 * <pre>
 * {@code
 * first(doStuff()).thenReturnEmpty();
 * }
 * </pre>
 */
public class FirstThenFlux {

    private Flux<?> doFirst;

    private FirstThenFlux(Flux<?> doFirst) {
        this.doFirst = doFirst;
    }

    public static FirstThenFlux first(Flux<?> doFirst) {
        return new FirstThenFlux(doFirst);
    }

    @SuppressWarnings("unchecked")
    private static <S> Flux<S> ignoreElements(Flux<?> toConsume) {
        return (Flux<S>)toConsume.ignoreElements().flux();
    }

    public <T> FirstThenFlux then(Flux<T> thenReturn) {
        return new FirstThenFlux(ignoreElements(doFirst).<T>concatWith(thenReturn));
    }

    public <T> FirstThenFlux then(Supplier<Flux<T>> thenFn) {
        return new FirstThenFlux(ignoreElements(doFirst).concatWith(Flux.defer(thenFn)));
    }

    public <T> Flux<T> thenReturn(Supplier<Flux<T>> thenFn) {
        return thenReturn(Flux.defer(thenFn));
    }

    public <T> Flux<T> thenReturn(T thenReturn) {
        return thenReturn(Flux.just(thenReturn));
    }

    public <T> Flux<T> thenReturn(Flux<T> thenReturn) {
        return FirstThenFlux.<T>ignoreElements(doFirst).concatWith(thenReturn);
    }

    /**
     * An empty observable, signalling success or error.
     */
    public <T> Flux<T> thenReturnEmpty() {
        return thenReturn(Flux.empty());
    }

}
