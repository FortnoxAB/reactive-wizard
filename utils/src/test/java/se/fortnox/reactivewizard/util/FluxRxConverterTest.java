package se.fortnox.reactivewizard.util;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.test.StepVerifier;
import rx.Observable;
import rx.Single;
import rx.subjects.Subject;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class FluxRxConverterTest {

    List<String> nbrs = asList("1", "2");
    String nbrSingle = "3";

    @Test
    void testConverterToFlux() {
        assertFluxNumbers(FluxRxConverter.<String>converterToFlux(Observable.class).apply(Observable.from(nbrs)));
        assertFluxNumbersSingle(FluxRxConverter.<String>converterToFlux(Single.class).apply(Single.just(nbrSingle)));
        assertFluxNumbers(FluxRxConverter.<String>converterToFlux(Flux.class).apply(Flux.fromIterable(nbrs)));
        assertFluxNumbersSingle(FluxRxConverter.<String>converterToFlux(Mono.class).apply(Mono.just(nbrSingle)));
        assertThat(FluxRxConverter.converterToFlux(String.class)).isNull();
    }

    @Test
    void testConverterFromObservable() {
        Observable<String> observableResult = (Observable<String>) FluxRxConverter.converterFromObservable(Observable.class).apply(Observable.from(nbrs));
        assertThat(observableResult.toList().toBlocking().single()).isEqualTo(nbrs);

        Single<String> singleResult = (Single<String>) FluxRxConverter.converterFromObservable(Single.class).apply(Observable.just(nbrSingle));
        assertThat(singleResult.toBlocking().value()).isEqualTo(nbrSingle);

        Flux<String> fluxResult = (Flux<String>) FluxRxConverter.converterFromObservable(Flux.class).apply(Observable.from(nbrs));
        assertThat(fluxResult.collectList().block()).isEqualTo(nbrs);

        Mono<String> monoResult = (Mono<String>) FluxRxConverter.converterFromObservable(Mono.class).apply(Observable.just(nbrSingle));
        assertThat(monoResult.block()).isEqualTo(nbrSingle);

        Mono<String> emptyMonoResult = (Mono<String>) FluxRxConverter.converterFromObservable(Mono.class).apply(Observable.empty());
        assertThat(emptyMonoResult.block()).isNull();
    }

    @Test
    void testConverterFromFlux() {
        Observable<String> observableResult = (Observable<String>) FluxRxConverter.converterFromFlux(Observable.class).apply(Flux.fromIterable(nbrs));
        assertThat(observableResult.toList().toBlocking().single()).isEqualTo(nbrs);

        Single<String> singleResult = (Single<String>) FluxRxConverter.converterFromFlux(Single.class).apply(Flux.just(nbrSingle));
        assertThat(singleResult.toBlocking().value()).isEqualTo(nbrSingle);

        Flux<String> fluxResult = (Flux<String>) FluxRxConverter.converterFromFlux(Flux.class).apply(Flux.fromIterable(nbrs));
        assertThat(fluxResult.collectList().block()).isEqualTo(nbrs);

        Mono<String> monoResult = (Mono<String>) FluxRxConverter.converterFromFlux(Mono.class).apply(Flux.just(nbrSingle));
        assertThat(monoResult.block()).isEqualTo(nbrSingle);
    }

    @Test
    void testIsReactiveType() {
        assertThat(FluxRxConverter.isReactiveType(Observable.class)).isTrue();
        assertThat(FluxRxConverter.isReactiveType(Subject.class)).isTrue();

        assertThat(FluxRxConverter.isReactiveType(Single.class)).isTrue();

        assertThat(FluxRxConverter.isReactiveType(Flux.class)).isTrue();

        assertThat(FluxRxConverter.isReactiveType(Mono.class)).isTrue();
        assertThat(FluxRxConverter.isReactiveType(MonoOperator.class)).isTrue();

        assertThat(FluxRxConverter.isReactiveType(String.class)).isFalse();
        assertThat(FluxRxConverter.isReactiveType(Object.class)).isFalse();

        assertThat(FluxRxConverter.isSingleType(Flux.class)).isFalse();
        assertThat(FluxRxConverter.isSingleType(Mono.class)).isTrue();
        assertThat(FluxRxConverter.isSingleType(Single.class)).isTrue();
        // From a convention for correct json responses, Observable is treated as a "SingleType" even though it is not
        assertThat(FluxRxConverter.isSingleType(Observable.class)).isTrue();
    }

    @Test
    void shouldThrowNPEIfObservableIsNullWhenConvertingToFlux() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> FluxRxConverter.observableToFlux(null))
            .withMessage("Observable to convert must not be null");
    }

    @Test
    void shouldThrowNPEIfObservableIsNullWhenConvertingToMono() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> FluxRxConverter.observableToMono(null))
            .withMessage("Observable to convert must not be null");
    }

    @Test
    void shouldKeepDecorationWhenConvertingFromObservableToFlux() {
        String value = "yo!";
        int decoration = 123;
        Observable<String> decoratedObservable = ReactiveDecorator.decorated(Observable.just(value), decoration);
        Function<Object, Flux<String>> converter = FluxRxConverter.converterToFlux(Observable.class);

        Flux<String> conversionResult = converter.apply(decoratedObservable);

        StepVerifier.create(conversionResult)
            .expectNext(value)
            .verifyComplete();
        assertThat(ReactiveDecorator.getDecoration(conversionResult))
            .hasValue(decoration);
    }

    @Test
    void shouldBeAbleToConvertObservableWithMoreThanOneItemToMono() {
        List<String> values = List.of("one", "two");
        Observable<String> observable = Observable.from(values);

        Mono<String> conversionResult = FluxRxConverter.observableToMono(observable);

        StepVerifier.create(conversionResult)
            .expectNext(values.get(0))
            .verifyComplete();
    }

    private void assertFluxNumbersSingle(Flux<String> flux) {
        assertThat(flux.single().block()).isEqualTo(nbrSingle);
    }

    private void assertFluxNumbers(Flux<String> flux) {
        assertThat(flux.collectList().block()).isEqualTo(nbrs);
    }
}
