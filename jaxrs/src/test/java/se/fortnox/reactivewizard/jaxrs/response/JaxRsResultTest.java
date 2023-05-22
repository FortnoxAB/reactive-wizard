package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.mocks.MockHttpServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JaxRsResultTest {

    @Test
    void shouldInvokeDoOnEmptyRunnableSuppliedWhenEmpty() {

        JaxRsResult jaxRsResult = new JaxRsResult(Flux.empty(), HttpResponseStatus.NO_CONTENT, flux -> flux, Collections.emptyMap());

        AtomicBoolean written = new AtomicBoolean();
        jaxRsResult.doOnEmpty(() -> {
            written.set(true);
        });

        Flux.from(jaxRsResult.write(new MockHttpServerResponse())).ignoreElements().block();

        assertThat(written.get()).isTrue();
    }

    @Test
    void shouldNotInvokeDoOnEmptyWhenFluxIsNotEmpty() {
        JaxRsResult jaxRsResult = new JaxRsResult(Flux.just("".getBytes(StandardCharsets.UTF_8)), HttpResponseStatus.OK, flux -> {
            return flux;
        }, Collections.emptyMap());

        AtomicBoolean written = new AtomicBoolean();
        jaxRsResult.doOnEmpty(() -> {
            written.set(true);
        });

        Flux.from(jaxRsResult.write(new MockHttpServerResponse())).ignoreElements().block();

        assertThat(written.get()).isFalse();
    }
}
