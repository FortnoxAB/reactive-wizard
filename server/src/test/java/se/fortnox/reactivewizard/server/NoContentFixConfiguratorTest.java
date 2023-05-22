package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class NoContentFixConfiguratorTest {

    @Test
    void shouldOnlyRemoveHeaderContentLengthIfNoBody() {
        RwServer rwServer = server((httpServerRequest, httpServerResponse) -> {
            httpServerResponse.status(HttpResponseStatus.OK);
            httpServerResponse.addHeader("Content-Length", "0");
            httpServerResponse.addHeader("random", "0");
            return Flux.empty();
        });

        try {
            HttpClientResponse response = HttpClient.create()
                .baseUrl("http://localhost:"+ rwServer.getServer().port()).get().response().block();

            assertThat(response.responseHeaders()).hasSize(1);
            assertThat(response.responseHeaders().get("random")).isEqualTo("0");
        } finally {
            rwServer.getServer().disposeNow();
        }
    }

    @Test
    void shouldRemoveTransferEncodingIfNoContent() throws Exception {
        RwServer rwServer = server((httpServerRequest, httpServerResponse) -> {
            httpServerResponse.status(HttpResponseStatus.NO_CONTENT);
            httpServerResponse.addHeader("Transfer-Encoding", "some encoding");
            return Flux.empty();
        });

        try {
            HttpClientResponse response = HttpClient.create()
                .baseUrl("http://localhost:"+ rwServer.getServer().port()).get().response().block();

            assertThat(response.responseHeaders()).isEmpty();
        } finally {
            rwServer.getServer().disposeNow();
        }
    }

    private RwServer server(RequestHandler handler) {
        ServerConfig config = new ServerConfig();
        config.setPort(0);
        ConnectionCounter connectionCounter = new ConnectionCounter();
        CompositeRequestHandler handlers = new CompositeRequestHandler(Collections.singleton(handler), new ExceptionHandler(new ObjectMapper(), new RequestLogger()), connectionCounter, new RequestLogger());
        return new RwServer(config, handlers, connectionCounter);
    }

}
