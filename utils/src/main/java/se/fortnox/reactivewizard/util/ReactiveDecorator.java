package se.fortnox.reactivewizard.util;

import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ReactiveDecorator {

    public static <R,S> BiFunction<Observable<R>, S, Object> converterFromObservableDecorated(Class<?> returnType) {
        if (Observable.class.isAssignableFrom(returnType)) {
            return (observable, state) -> new DecoratedObservable(observable, state);
        } else if (Flux.class.isAssignableFrom(returnType)) {
            return (observable, state) -> new DecoratedFlux(RxReactiveStreams.toPublisher(observable), state);
        } else {
            throw new IllegalArgumentException("Return type needs to be a reactive type (Flux, Mono, Observable, Single) but was " + returnType);
        }
    }

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

    public static <T> Optional<T> getDecoration(Observable<?> wrapper) {
        if (!(wrapper instanceof DecoratedObservable)) {
            return Optional.empty();
        }
        return Optional.of((T) ((DecoratedObservable)wrapper).getDecoration());
    }

    public static <T> Optional<T> getDecoration(Single<?> wrapper) {
        if (!(wrapper instanceof DecoratedSingle)) {
            return Optional.empty();
        }
        return Optional.of((T) ((DecoratedSingle)wrapper).getDecoration());
    }

    public static <T> Optional<T> getDecoration(Flux<?> wrapper) {
        if (!(wrapper instanceof DecoratedFlux)) {
            return Optional.empty();
        }
        return Optional.of((T) ((DecoratedFlux)wrapper).getDecoration());
    }

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
}
