package se.fortnox.reactivewizard.jaxrs;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import se.fortnox.reactivewizard.ExceptionHandler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FluxResourceTest {
    ByteBufCollector byteBufCollector = new ByteBufCollector();
    FluxResource fluxResource = mock(FluxResource.class);
    JaxRsRequestHandler handler = new JaxRsRequestHandler(new Object[]{fluxResource}, new JaxRsResourceFactory(), new ExceptionHandler(), false);
    HttpClient httpClient = HttpClient.create();

    @Test
    public void shouldReturnJsonArrayForFlux() {
        when(fluxResource.arrayOfStrings()).thenReturn(Flux.just("a", "b"));
        assertResponse("/flux/strings", "[\"a\",\"b\"]");
    }

    @Test
    public void shouldReturnEmptyArray() {
        when(fluxResource.arrayOfStrings()).thenReturn(Flux.empty());
        assertResponse("/flux/strings", "[]");
    }

    @Test
    public void shouldReturnJsonObjectForMono() {
        when(fluxResource.monoString()).thenReturn(Mono.just("a"));
        assertResponse("/flux/string", "\"a\"");
    }

    private void assertResponse(String path, String expectedResponse) {
        DisposableServer server = HttpServer.create().handle(handler).bindNow();
        try {
            String response = byteBufCollector.collectString(httpClient.get()
                .uri("http://localhost:" + server.port() + path)
                .responseContent())
                .block();

            assertThat(response).isEqualTo(expectedResponse);
        } finally {
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
    }
}
