package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResultFactoryFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.netty.handler.codec.http.HttpResponseStatus.PAYMENT_REQUIRED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FluxResourceTest {
    FluxResource fluxResource = mock(FluxResource.class);
    JaxRsRequestHandler handler = new JaxRsRequestHandler(new Object[]{fluxResource}, new JaxRsResourceFactory(), new ExceptionHandler(), false);

    @Test
    void shouldReturnJsonArrayForFlux() {
        when(fluxResource.arrayOfStrings()).thenReturn(Flux.just("a", "b"));
        FluxResourceAsserter.from(handler).sendRequest("/flux/strings")
            .assertResponse("[\"a\",\"b\"]")
            .close();
    }

    @Test
    void shouldReturnEmptyArray() {
        when(fluxResource.arrayOfStrings()).thenReturn(Flux.empty());
        FluxResourceAsserter.from(handler).sendRequest("/flux/strings").assertResponse("[]").close();
    }

    @Test
    void shouldReturnError() {
        when(fluxResource.arrayOfStrings()).thenReturn(Flux.error(new WebException(PAYMENT_REQUIRED, "error")));
        FluxResourceAsserter.from(handler).sendRequest("/flux/strings").assertStatus(PAYMENT_REQUIRED).close();
    }

    @Test
    void shouldReturnJsonObjectForMono() {
        when(fluxResource.monoString()).thenReturn(Mono.just("a"));
        FluxResourceAsserter.from(handler)
            .sendRequest("/flux/string")
            .assertResponse("\"a\"")
            .close();
    }

    @Test
    void shouldStreamJsonArrayWhenStreamAnnotationIsPresent() {
        when(fluxResource.jsonArrayStreamOfTestEntities()).thenReturn(Flux.just(new TestEntity("Hello"), new TestEntity("World")));
        FluxResourceAsserter.from(handler)
            .sendRequest("/flux/json-array-stream-of-test-entities")
            .assertHeaderValue("Transfer-Encoding", "chunked")
            .assertResponse("[{\"value\":\"Hello\"},{\"value\":\"World\"}]")
            .close();
    }

    @Test
    void shouldStreamConcatenatedJsonObjectsWhenStreamAnnotationWithCoJsonIsPresent() {
        when(fluxResource.concatenatedJsonObjectsStreamOfTestEntities())
            .thenReturn(Flux.just(new TestEntity("Hello"), new TestEntity("World")));

        FluxResourceAsserter.from(handler)
            .sendRequest("/flux/concatenated-json-objects-stream-of-test-entities")
            .assertHeaderValue("Transfer-Encoding", "chunked")
            .assertHeaderValue("Content-Type", "application/json")
            .assertResponse("{\"value\":\"Hello\"}{\"value\":\"World\"}")
            .close();
    }

    @Test
    void shouldNotStreamWhenStreamAnnotationIsNotPresent() {
        when(fluxResource.nonStreamingTestEntities()).thenReturn(Flux.just(new TestEntity("Hello"), new TestEntity("World")));
        FluxResourceAsserter.from(handler)
            .sendRequest("/flux/non-streaming-test-entities")
            .assertHeaderValue("Transfer-Encoding", headerValue ->
                Assertions.assertThat(headerValue).isNotEqualTo("chunked"))
            .assertResponse("[{\"value\":\"Hello\"},{\"value\":\"World\"}]")
            .close();
    }

    @Test
    void shouldStreamConcatenatedJsonStringsWhenStreamAnnotationWithCoJsonIsPresent() {
        when(fluxResource.concatenatedJsonObjectsStreamOfStrings()).thenReturn(Flux.just("Hello", "World"));
        FluxResourceAsserter.from(handler)
            .sendRequest("/flux/concatenated-json-objects-stream-of-strings")
            .assertResponse("\"Hello\"\"World\"")
            .close();
    }

    @Test
    void shouldStreamConcatenatedStringsWhenStreamAnnotationWithCoJsonAndPlainTextIsPresent() {
        when(fluxResource.concatenatedJsonObjectsOfStringsWithTextPlainContentType())
            .thenReturn(Flux.just("Hello", "World"));

        FluxResourceAsserter.from(handler)
            .sendRequest("/flux/concatenated-json-objects-of-strings-with-text-plain-content-type")
            .assertResponse("HelloWorld")
            .close();
    }

    static class FluxResourceAsserter {
        private final DisposableServer server;
        private final ByteBufCollector byteBufCollector;

        private final HttpClient httpClient;

        private HttpClient.ResponseReceiver<?> responseReceiver;

        protected FluxResourceAsserter(JaxRsRequestHandler jaxRsRequestHandler, ByteBufCollector byteBufCollector, HttpClient httpClient) {
            server = HttpServer.create().handle(jaxRsRequestHandler).bindNow();
            this.byteBufCollector = byteBufCollector;
            this.httpClient = httpClient;
        }

        public static FluxResourceAsserter from(JaxRsRequestHandler jaxRsRequestHandler) {
            return new FluxResourceAsserter(jaxRsRequestHandler, new ByteBufCollector(), HttpClient.create());
        }

        public FluxResourceAsserter sendRequest(String path) {
            this.responseReceiver = httpClient.get().uri("http://localhost:" + server.port() + path);
            return this;
        }

        public FluxResourceAsserter assertResponse(String expectedResponse) {
            Mono<String> response = byteBufCollector.collectString(responseReceiver.responseContent());
            StepVerifier.create(response).expectNext(expectedResponse).verifyComplete();
            return this;
        }

        public FluxResourceAsserter assertStatus(HttpResponseStatus httpResponseStatus) {
            Mono<HttpResponseStatus> responseStatus = responseReceiver.response().map(HttpClientResponse::status);
            StepVerifier.create(responseStatus).expectNext(httpResponseStatus).verifyComplete();
            return this;
        }

        public FluxResourceAsserter assertHeaderValue(String header, String value) {
            Mono<String> headerValue = responseReceiver.response()
                .map(HttpClientResponse::responseHeaders)
                .map(httpHeaders -> httpHeaders.get(header))
                .onErrorResume(throwable -> Mono.just(""));

            StepVerifier.create(headerValue).expectNext(value).verifyComplete();
            return this;
        }

        public FluxResourceAsserter assertHeaderValue(String header, Consumer<String> asserter) {
            Mono<String> headerValue = responseReceiver.response()
                .map(HttpClientResponse::responseHeaders)
                .map(httpHeaders -> httpHeaders.get(header))
                .onErrorResume(throwable -> Mono.just(""));

            StepVerifier.create(headerValue).assertNext(asserter).verifyComplete();
            return this;
        }

        public void close() {
            server.disposeNow();
        }

    }


    @Path("/flux")
    interface FluxResource {
        @GET
        @Path("strings")
        Flux<String> arrayOfStrings();

        @GET
        @Path("string")
        Mono<String> monoString();

        @GET
        @Path("non-streaming-test-entities")
        Flux<TestEntity> nonStreamingTestEntities();

        @GET
        @Stream
        @Path("json-array-stream-of-test-entities")
        Flux<TestEntity> jsonArrayStreamOfTestEntities();


        @GET
        @Stream(Stream.Type.CONCATENATED_JSON_OBJECTS)
        @Path("concatenated-json-objects-stream-of-test-entities")
        Flux<TestEntity> concatenatedJsonObjectsStreamOfTestEntities();

        @GET
        @Stream(Stream.Type.CONCATENATED_JSON_OBJECTS)
        @Path("concatenated-json-objects-stream-of-strings")
        Flux<String> concatenatedJsonObjectsStreamOfStrings();

        @GET
        @Stream(Stream.Type.CONCATENATED_JSON_OBJECTS)
        @Path("concatenated-json-objects-of-strings-with-text-plain-content-type")
        @Produces(MediaType.TEXT_PLAIN)
        Flux<String> concatenatedJsonObjectsOfStringsWithTextPlainContentType();
    }

    @Path("/failing-resource-flux")
    interface FailingResource {
        @GET
        @Produces("application/octet-stream")
        Flux<byte[]> nonStreamingOctetStream();
    }

    static class FailingResourceImpl implements FailingResource {
        @Override
        public Flux<byte[]> nonStreamingOctetStream() {
            return Flux.empty();
        }
    }

    public static class TestEntity {
        private String value;

        public TestEntity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
