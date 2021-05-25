package se.fortnox.reactivewizard.util;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import rx.Observable;
import rx.Single;
import rx.subjects.Subject;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static rx.Observable.just;

public class FluxRxConverterTest {

    List<String> nbrs = asList("1", "2");
    String nbrSingle = "3";

    @Test
    public void testConverterToFlux() {
        assertFluxNumbers(FluxRxConverter.<String>converterToFlux(Observable.class).apply(Observable.from(nbrs)));
        assertFluxNumbersSingle(FluxRxConverter.<String>converterToFlux(Single.class).apply(Single.just(nbrSingle)));
        assertFluxNumbers(FluxRxConverter.<String>converterToFlux(Flux.class).apply(Flux.fromIterable(nbrs)));
        assertFluxNumbersSingle(FluxRxConverter.<String>converterToFlux(Mono.class).apply(Mono.just(nbrSingle)));
        assertThat(FluxRxConverter.converterToFlux(String.class)).isNull();
    }

    @Test
    public void testConverterFromObservable() {
        Observable<String> observableResult = (Observable<String>) FluxRxConverter.converterFromObservable(Observable.class).apply(Observable.from(nbrs));
        assertThat(observableResult.toList().toBlocking().single()).isEqualTo(nbrs);

        Single<String> singleResult = (Single<String>) FluxRxConverter.converterFromObservable(Single.class).apply(Observable.just(nbrSingle));
        assertThat(singleResult.toBlocking().value()).isEqualTo(nbrSingle);

        Flux<String> fluxResult = (Flux<String>) FluxRxConverter.converterFromObservable(Flux.class).apply(Observable.from(nbrs));
        assertThat(fluxResult.collectList().block()).isEqualTo(nbrs);

        Mono<String> monoResult = (Mono<String>) FluxRxConverter.converterFromObservable(Mono.class).apply(Observable.just(nbrSingle));
        assertThat(monoResult.block()).isEqualTo(nbrSingle);
    }

    @Test
    public void testIsReactiveType() {
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

    private void assertFluxNumbersSingle(Flux<String> flux) {
        assertThat(flux.single().block()).isEqualTo(nbrSingle);
    }

    private void assertFluxNumbers(Flux<String> flux) {
        assertThat(flux.collectList().block()).isEqualTo(nbrs);
    }
}
