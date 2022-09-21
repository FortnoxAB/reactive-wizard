package se.fortnox.reactivewizard.util;

import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import java.util.Optional;
import java.util.function.Function;

public class ReactiveDecorator {

    /**
     * Decorate reactive type.
     * @param inner the reactive type to decorate
     * @param decoration the decoration
     * @param <T> reactive type
     * @param <S> decoration type
     * @return the decorated reactive type
     */
    public static <T,S> T decorated(T inner, S decoration) {
        if (inner instanceof Observable) {
            return (T)new DecoratedObservable((Observable) inner, decoration);
        } else if (inner instanceof Flux) {
            return (T)new DecoratedFlux<>((Flux) inner, decoration);
        } else if (inner instanceof Single) {
            return (T)new DecoratedSingle<>((Single)inner, decoration);
        } else if (inner instanceof Mono) {
            return (T)new DecoratedMono<>((Mono)inner, decoration);
        }
        throw new IllegalArgumentException("Type to decorate needs to be a reactive type (Flux, Mono, Observable, Single) but was " + inner.getClass());
    }

    /**
     * Get the decoration of an Observable.
     * @param wrapper the Observable
     * @param <T> the type of the decoration
     * @return an Optional containing the decoration, if any
     */
    public static <T> Optional<T> getDecoration(Observable<?> wrapper) {
        if (!(wrapper instanceof DecoratedObservable)) {
            return Optional.empty();
        }
        return Optional.of((T) ((DecoratedObservable)wrapper).getDecoration());
    }

    /**
     * Get the decoration of a Single.
     * @param wrapper the Single
     * @param <T> the type of the decoration
     * @return an Optional containing the decoration, if any
     */
    public static <T> Optional<T> getDecoration(Single<?> wrapper) {
        if (!(wrapper instanceof DecoratedSingle)) {
            return Optional.empty();
        }
        return Optional.of((T) ((DecoratedSingle)wrapper).getDecoration());
    }

    /**
     * Get the decoration of a Flux.
     * @param wrapper the Flux
     * @param <T> the type of the decoration
     * @return an Optional containing the decoration, if any
     */
    public static <T> Optional<T> getDecoration(Flux<?> wrapper) {
        if (!(wrapper instanceof DecoratedFlux)) {
            return Optional.empty();
        }
        return Optional.of((T) ((DecoratedFlux)wrapper).getDecoration());
    }

    /**
     * Get the decoration of a Publisher.
     * @param wrapper the Publisher
     * @param <T> the type of the decoration
     * @return an Optional containing the decoration, if any
     */
    public static <T> Optional<T> getDecoration(Publisher<?> wrapper) {
        if (wrapper instanceof DecoratedFlux decoratedFlux) {
            return getDecoration(decoratedFlux);
        }

        if (wrapper instanceof DecoratedMono decoratedMono) {
            return getDecoration(decoratedMono);
        }

        return Optional.empty();
    }

    /**
     * Get the decoration of a Mono.
     * @param wrapper the Mono
     * @param <T> the type of the decoration
     * @return an Optional containing the decoration, if any
     */
    public static <T> Optional<T> getDecoration(Mono<?> wrapper) {
        if (!(wrapper instanceof DecoratedMono)) {
            return Optional.empty();
        }
        return Optional.of((T) ((DecoratedMono)wrapper).getDecoration());
    }

    private static class DecoratedObservable<T,S> extends Observable<T> {
        private final S decoration;

        public DecoratedObservable(Observable<T> inner, S decoration) {
            super(inner::unsafeSubscribe);
            this.decoration = decoration;
        }

        public S getDecoration() {
            return decoration;
        }
    }

    private static class DecoratedSingle<T,S> extends Single<T> {
        private final S decoration;

        public DecoratedSingle(Single<T> inner, S decoration) {
            super((OnSubscribe<T>)inner::subscribe);
            this.decoration = decoration;
        }

        public S getDecoration() {
            return decoration;
        }
    }

    private static class DecoratedFlux<T,S> extends Flux<T> {
        private final S decoration;
        private final Publisher<T> inner;

        public DecoratedFlux(Publisher<T> inner, S decoration) {
            this.inner = inner;
            this.decoration = decoration;
        }

        @Override
        public void subscribe(CoreSubscriber<? super T> coreSubscriber) {
            inner.subscribe(coreSubscriber);
        }

        public S getDecoration() {
            return decoration;
        }
    }

    private static class DecoratedMono<T,S> extends Mono<T> {
        private final S decoration;
        private final Publisher<T> inner;

        public DecoratedMono(Publisher<T> inner, S decoration) {
            this.inner = inner;
            this.decoration = decoration;
        }

        @Override
        public void subscribe(CoreSubscriber<? super T> coreSubscriber) {
            inner.subscribe(coreSubscriber);
        }

        public S getDecoration() {
            return decoration;
        }
    }

    public static <T,R> Observable<R> keepDecoration(Observable<T> source, Function<Observable<T>, Observable<R>> transformation) {
        return new DecoratedObservable<>(transformation.apply(source), getDecoration(source).orElse(null));
    }

    public static <T,R> Flux<R> keepDecoration(Flux<T> source, Function<Flux<T>, Flux<R>> transformation) {
        return new DecoratedFlux<>(transformation.apply(source), getDecoration(source).orElse(null));
    }

    public static <T,R> Mono<R> keepDecoration(Mono<T> source, Function<Mono<T>, Mono<R>> transformation) {
        return new DecoratedMono<>(transformation.apply(source), getDecoration(source).orElse(null));
    }

    public static <T,R> Single<R> keepDecoration(Single<T> source, Function<Single<T>, Single<R>> transformation) {
        return new DecoratedSingle<>(transformation.apply(source), getDecoration(source).orElse(null));
    }

}
