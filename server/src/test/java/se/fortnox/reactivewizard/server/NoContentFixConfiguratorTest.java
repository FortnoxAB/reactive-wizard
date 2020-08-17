package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NoContentFixConfiguratorTest {

    @Test
    public void shouldAddContentFixToPipelinesNotAlreadyHavingThisFix() {
        NoContentFixConfigurator noContentFixConfigurator = new NoContentFixConfigurator();

        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        noContentFixConfigurator.call(pipeline);
        verify(pipeline).addBefore(anyString(), eq("NoContentFix"), any(NoContentFixConfigurator.NoContentBodyFix.class));
    }

    @Test
    public void shouldNotAddContentFixToPipelinesAlreadyHavingThisFix() {
        NoContentFixConfigurator noContentFixConfigurator = new NoContentFixConfigurator();

        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        when(pipeline.get("NoContentFix")).thenReturn(new NoContentFixConfigurator.NoContentBodyFix());
        verify(pipeline, never()).addBefore(anyString(), eq("NoContentFix"), any(NoContentFixConfigurator.NoContentBodyFix.class));
        noContentFixConfigurator.call(pipeline);
    }

    @Test
    public void shouldOnlyRemoveHeaderContentLengthIfNoBody() {
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
    public void shouldRemoveTransferEncodingIfNoContent() throws Exception {
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
        CompositeRequestHandler handlers = new CompositeRequestHandler(Collections.singleton(handler), new ExceptionHandler(new ObjectMapper()), connectionCounter);
        return new RwServer(config, handlers, connectionCounter);
    }

}
