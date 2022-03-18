package se.fortnox.reactivewizard.util;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ReactiveDecoratorTest {
    Object decoration = new Object();

    @Test
    public void canGetDecorationFromDecoratedObservable() {
        Observable<String> decorated = ReactiveDecorator.decorated(Observable.just("test"), decoration);

        assertThat(decorated).isNotNull();
        assertThat(decorated.toBlocking().single()).isEqualTo("test");

        Optional<Object> foundDecoration = ReactiveDecorator.getDecoration(decorated);
        assertThat(foundDecoration).containsSame(decoration);
    }

    @Test
    public void canGetDecorationFromDecoratedFlux() {
        Flux<String> decorated = ReactiveDecorator.decorated(Flux.just("test"), decoration);

        assertThat(decorated.single().block()).isEqualTo("test");
        assertThat(ReactiveDecorator.getDecoration(decorated)).containsSame(decoration);
    }

    @Test
    public void canGetDecorationFromDecoratedMono() {
        Mono<String> decorated = ReactiveDecorator.decorated(Mono.just("test"), decoration);

        assertThat(decorated.block()).isEqualTo("test");
        assertThat(ReactiveDecorator.getDecoration(decorated)).containsSame(decoration);
    }

    @Test
    public void canGetDecorationFromDecoratedSingle() {
        Single<String> decorated = ReactiveDecorator.decorated(Single.just("test"), decoration);

        assertThat(decorated.toBlocking().value()).isEqualTo("test");
        assertThat(ReactiveDecorator.getDecoration(decorated)).containsSame(decoration);
    }

    @Test
    public void willGetEmptyOptionalForNonDecorated() {
        assertThat(ReactiveDecorator.getDecoration(Observable.just("hej"))).isEmpty();
        assertThat(ReactiveDecorator.getDecoration(Flux.just("hej"))).isEmpty();
        assertThat(ReactiveDecorator.getDecoration(Mono.just("hej"))).isEmpty();
        assertThat(ReactiveDecorator.getDecoration(Single.just("hej"))).isEmpty();
    }
}
