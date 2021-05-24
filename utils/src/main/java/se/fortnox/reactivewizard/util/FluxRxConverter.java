package se.fortnox.reactivewizard.util;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import java.util.function.Function;

public class FluxRxConverter {

    public static <T> Flux<T> observableToFlux(Observable<T> result) {
        return Flux.from(RxReactiveStreams.toPublisher(result));
    }

    public static <T> Function<Object, Flux<T>> converterToFlux(Class<?> returnType) {

        if (Flux.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }
                return (Flux<T>)result;
            };

        } else if (Mono.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }
                return ((Mono<T>)result).flux();
            };
        } else if (Observable.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }
                return observableToFlux((Observable<T>)result);
            };
        } else if (Single.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }

                return Flux.from(RxReactiveStreams.toPublisher((Single<T>)result));
            };
        } else {
            return null;
        }
    }

    public static boolean isReactiveType(Class<?> returnType) {
        return Observable.class.isAssignableFrom(returnType)
                || Single.class.isAssignableFrom(returnType)
                || Flux.class.isAssignableFrom(returnType)
                || Mono.class.isAssignableFrom(returnType);
    }

    public static boolean isSingleType(Class<?> returnType) {
        return !Flux.class.isAssignableFrom(returnType);
    }

    public static Function<Observable<Object>, Object> converterFromObservable(Class<?> targetReactiveType) {
        if (Observable.class.isAssignableFrom(targetReactiveType)) {
            return result -> result;
        } else if (Single.class.isAssignableFrom(targetReactiveType)) {
            return Observable::toSingle;
        } else if (Flux.class.isAssignableFrom(targetReactiveType)) {
            return FluxRxConverter::observableToFlux;
        } else if (Mono.class.isAssignableFrom(targetReactiveType)) {
            return result -> observableToFlux(result).single();
        }
        return null;
    }
}
