package se.fortnox.reactivewizard.client;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import java.net.URISyntaxException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class FluxClientTest {
    FluxResource fluxServerResource = mock(FluxResource.class);
    ObservableResource observableServerResource = mock(ObservableResource.class);
    JaxRsRequestHandler handler = new JaxRsRequestHandler(new Object[]{fluxServerResource}, new JaxRsResourceFactory(), new ExceptionHandler(), false);

    @Test
    public void shouldDecodeJsonArrayAsFlux() throws URISyntaxException {
        DisposableServer server = HttpServer.create().handle(handler).bindNow();
        when(fluxServerResource.arrayOfStrings()).thenReturn(Flux.just("a", "b"));
        when(fluxServerResource.arrayOfEntities()).thenReturn(Flux.just(new Entity(), new Entity(), new Entity()));

        try {
            FluxResource fluxClientResource = client(server);

            List<String> result = fluxClientResource.arrayOfStrings().collectList().block();
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEqualTo("a");
            assertThat(result.get(1)).isEqualTo("b");

            List<Entity> entitiesResult = fluxClientResource.arrayOfEntities().collectList().block();
            assertThat(entitiesResult).hasSize(3);
            assertThat(entitiesResult.get(0).getSomeDouble()).isEqualTo(3.1415d);

        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldDecodeJsonArrayAsFluxInOneChunk() throws URISyntaxException {
        handler = new JaxRsRequestHandler(new Object[]{observableServerResource}, new JaxRsResourceFactory(), new ExceptionHandler(), false);
        DisposableServer server = HttpServer.create().handle(handler).bindNow();
        when(observableServerResource.arrayOfStrings()).thenReturn(Observable.just("a", "b").toList());

        try {
            FluxResource fluxClientResource = client(server);
            List<String> result = fluxClientResource.arrayOfStrings().collectList().block();
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEqualTo("a");
            assertThat(result.get(1)).isEqualTo("b");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldDecodeJsonArrayAsFluxInMultipleChunks() throws URISyntaxException {
        DisposableServer server = HttpServer.create().handle((req, resp)->{
            resp.header(CONTENT_TYPE, APPLICATION_JSON);
            return resp.sendString(Flux.just(
                "[   \t",
                "\"",
                "a\"\r\n\r\n,",
                "\"b",
                "\"]"));
        }).bindNow();

        try {
            FluxResource fluxClientResource = client(server);
            List<String> result = fluxClientResource.arrayOfStrings().collectList().block();
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isEqualTo("a");
            assertThat(result.get(1)).isEqualTo("b");
        } finally {
            server.disposeNow();
        }
    }


    @Test
    public void shouldDecodeJsonObjectAsMonoInMultipleChunks() throws URISyntaxException {
        DisposableServer server = HttpServer.create().handle((req, resp)->{
            resp.header(CONTENT_TYPE, APPLICATION_JSON);
            return resp.sendString(Flux.just(
                "{\"some",
                "Double\":4.2,\n\"s",
                "omeString\":\"a",
                "bc\"}"));
        }).bindNow();

        try {
            FluxResource fluxClientResource = client(server);
            Entity result = fluxClientResource.monoEntity().block();
            assertThat(result.getSomeDouble()).isEqualTo(4.2);
            assertThat(result.getSomeString()).isEqualTo("abc");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldSupportBackpressure() throws URISyntaxException {
        DisposableServer server = HttpServer.create().handle(handler).bindNow();
        AtomicInteger emitted = new AtomicInteger();
        when(fluxServerResource.arrayOfStrings()).thenReturn(Flux.just("aaaaaa").repeat(10000).doOnNext(value->emitted.incrementAndGet()));

        try {
            FluxResource fluxClientResource = client(server);

            List<String> result = fluxClientResource.arrayOfStrings().take(2).collectList().block();
            assertThat(result).hasSize(2);

            // It seems to become 152 requests all the time, but the important part is that it will avoid emitting all
            // 10000 strings, so if this fails in the future we could change it to ensure that is less than 1000 or so
            assertThat(emitted.get()).isLessThanOrEqualTo(152);
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldSupportGettingResponseHeadersFromFlux() throws URISyntaxException {
        DisposableServer server = HttpServer.create().handle(handler).bindNow();
        AtomicInteger subscriptions = new AtomicInteger();
        Flux<String> resultToReturn = Flux.just("a", "b").doOnSubscribe(s->subscriptions.incrementAndGet());
        when(fluxServerResource.arrayOfStrings()).thenReturn(resultToReturn);

        try {
            FluxResource fluxClientResource = client(server);

            Flux<String> result = fluxClientResource.arrayOfStrings();
            Mono<Response<Flux<String>>> fullResponse = HttpClient.getFullResponse(result);
            Response<Flux<String>> awaitedResponse = fullResponse.block();
            assertThat(awaitedResponse.getStatus()).isEqualTo(HttpResponseStatus.OK);
            assertThat(awaitedResponse.getHeaders()).isNotEmpty();

            List<String> bodyResult = awaitedResponse.getBody().collectList().block();
            assertThat(bodyResult).hasSize(2);
            assertThat(bodyResult.get(0)).isEqualTo("a");

            verify(fluxServerResource).arrayOfStrings();

            assertThat(subscriptions.get()).isEqualTo(1);
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldSupportGettingResponseHeadersFromMono() throws URISyntaxException {
        DisposableServer server = HttpServer.create().handle(handler).bindNow();
        when(fluxServerResource.monoString()).thenReturn(Mono.just("hej"));

        try {
            FluxResource fluxClientResource = client(server);

            Mono<String> result = fluxClientResource.monoString();
            Mono<Response<String>> fullResponse = HttpClient.getFullResponse(result);
            Response<String> awaitedResponse = fullResponse.block();
            assertThat(awaitedResponse.getStatus()).isEqualTo(HttpResponseStatus.OK);
            assertThat(awaitedResponse.getHeaders()).isNotEmpty();
            assertThat(awaitedResponse.getBody()).isEqualTo("hej");
        } finally {
            server.disposeNow();
        }
    }

    private FluxResource client(DisposableServer server) throws URISyntaxException {
        HttpClientConfig config = new HttpClientConfig("http://localhost:"+server.port());
        config.setReadTimeoutMs(1000000);
        HttpClient client = new HttpClient(config);
        client.setTimeout(10, ChronoUnit.MINUTES);
        return client.create(FluxResource.class);
    }

    @Path("/")
    interface FluxResource {
        @GET
        @Path("strings")
        Flux<String> arrayOfStrings();

        @GET
        @Path("entities")
        Flux<Entity> arrayOfEntities();

        @GET
        @Path("string")
        Mono<String> monoString();

        @GET
        @Path("entity")
        Mono<Entity> monoEntity();

    }

    @Path("/")
    interface ObservableResource {
        @GET
        @Path("strings")
        Observable<List<String>> arrayOfStrings();

        @GET
        @Path("entities")
        Observable<List<Entity>> arrayOfEntities();
    }

    public static class Entity {
        private double someDouble = 3.1415;
        private String someString = "åäö123";

        public double getSomeDouble() {
            return someDouble;
        }

        public void setSomeDouble(double someDouble) {
            this.someDouble = someDouble;
        }

        public String getSomeString() {
            return someString;
        }

        public void setSomeString(String someString) {
            this.someString = someString;
        }
    }
}
