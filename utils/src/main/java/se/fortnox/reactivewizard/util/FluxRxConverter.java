package se.fortnox.reactivewizard.util;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class FluxRxConverter {

    public static final String OBSERVABLE_NULL_ERROR = "Observable to convert must not be null";

    /**
     * <p>Convert Observable to Flux.</p>
     * <p>Note that this method:</p>
     * <ul>
     *     <li>Does not preserve any decoration present on the Observable. For that purpose, use {@link #converterToFlux(Class)}.</li>
     * </ul>
     *
     * @param observable the Observable to convert
     * @param <T>        type of the Observable
     * @return the converted Flux
     */
    public static <T> Flux<T> observableToFlux(Observable<T> observable) {
        requireNonNull(observable, OBSERVABLE_NULL_ERROR);
        return Flux.from(RxReactiveStreams.toPublisher(observable));
    }

    /**
     * <p>Convert Observable to Mono.</p>
     * <p>Note that this method:</p>
     * <ul>
     *     <li>Does not preserve any decoration present on the Observable.</li>
     *     <li>Will only keep the first (or none, if empty) value emitted by the source Observable.</li>
     * </ul>
     *
     * @param observable the Observable to convert
     * @param <T>        type of the Observable
     * @return the converted Mono
     */
    public static <T> Mono<T> observableToMono(Observable<T> observable) {
        requireNonNull(observable, OBSERVABLE_NULL_ERROR);
        return Mono.from(RxReactiveStreams.toPublisher(observable));
    }

    /**
     * Create converter from a reactive type to Flux, keeping any decoration present in the source object.
     *
     * @param returnType the return type
     * @param <T>        the type of Flux
     * @return the converter or null if unable to create converter
     */
    public static <T> Function<Object, Flux<T>> converterToFlux(Class<?> returnType) {

        if (Flux.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }
                return (Flux<T>) result;
            };

        } else if (Mono.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }
                Flux<T> flux = ((Mono<T>) result).flux();
                return ReactiveDecorator.getDecoration((Mono) result)
                    .map(state -> ReactiveDecorator.decorated(flux, state))
                    .orElse(flux);
            };
        } else if (Observable.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }
                Flux<T> flux = observableToFlux((Observable<T>) result);
                return ReactiveDecorator.getDecoration((Observable<?>) result)
                    .map(state -> ReactiveDecorator.decorated(flux, state))
                    .orElse(flux);
            };
        } else if (Single.class.isAssignableFrom(returnType)) {
            return result -> {
                if (result == null) {
                    return Flux.empty();
                }

                Flux<T> flux = Flux.from(RxReactiveStreams.toPublisher((Single<T>) result));
                return ReactiveDecorator.getDecoration((Single<?>) result)
                    .map(state -> ReactiveDecorator.decorated(flux, state))
                    .orElse(flux);
            };
        } else {
            return null;
        }
    }

    /**
     * Create converter from Flux to a reactive type.
     *
     * @param returnType the return type
     * @return the converter
     */
    public static Function<Flux, Object> converterFromFlux(Class<?> returnType) {
        if (Observable.class.isAssignableFrom(returnType)) {
            return RxReactiveStreams::toObservable;
        } else if (Single.class.isAssignableFrom(returnType)) {
            return RxReactiveStreams::toSingle;
        } else if (Flux.class.isAssignableFrom(returnType)) {
            return flux -> flux;
        } else if (Mono.class.isAssignableFrom(returnType)) {
            return Mono::from;
        } else {
            throw new IllegalArgumentException("Only Observable/Single and Flux/Mono return types are implemented for converters");
        }
    }

    /**
     * Determine if a Class is of a reactive type that can be handled by this class.
     *
     * @param returnType the type to check
     * @return whether the type is reactive
     */
    public static boolean isReactiveType(Class<?> returnType) {
        return Observable.class.isAssignableFrom(returnType)
            || Single.class.isAssignableFrom(returnType)
            || Flux.class.isAssignableFrom(returnType)
            || Mono.class.isAssignableFrom(returnType);
    }

    public static boolean isSingleType(Class<?> returnType) {
        return !Flux.class.isAssignableFrom(returnType);
    }

    public static boolean isFlux(Class<?> returnType) {
        return Flux.class.isAssignableFrom(returnType);
    }

    /**
     * Create a converter from Observable to a reactive type.
     *
     * @param targetReactiveType class of reactive type
     * @return the converter
     */
    public static Function<Observable<Object>, Object> converterFromObservable(Class<?> targetReactiveType) {
        if (Observable.class.isAssignableFrom(targetReactiveType)) {
            return result -> result;
        } else if (Single.class.isAssignableFrom(targetReactiveType)) {
            return Observable::toSingle;
        } else if (Flux.class.isAssignableFrom(targetReactiveType)) {
            return FluxRxConverter::observableToFlux;
        } else if (Mono.class.isAssignableFrom(targetReactiveType)) {
            return FluxRxConverter::observableToMono;
        }
        return null;
    }

    private FluxRxConverter() {
    }
}
