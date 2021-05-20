package se.fortnox.reactivewizard.jaxrs;

import org.junit.Test;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator.withHeaders;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.testServer;

public class StreamingDataTest {
    private StreamingResource   streamingResource   = new StreamingResourceImpl();
    private NoStreamingResource noStreamingResource = new NoStreamingResourceImpl();

    @Test
    public void testStreamingWithRealServer() {

        DisposableServer      server   = testServer(streamingResource, noStreamingResource).getServer();
        HttpClient            client   = HttpClient.create().port(server.port());
        final AtomicReference<HttpClientResponse>    response = new AtomicReference<>();
        List<String> strings = client.get().uri("/stream").response((resp, body)->{
            response.set(resp);
            return body.asString();
        }).collectList().block();

        assertThat(strings).hasSize(2);
        assertThat(strings.get(0)).isEqualTo("a");
        assertThat(strings.get(1)).isEqualTo("b");
        assertThat(response.get().responseHeaders().get("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);

        //When not streaming the response will finish after first string emission
        strings = client.get().uri("/nostream").response((resp, body)->{
            response.set(resp);
            return body.asString();
        }).collectList().block();

        assertThat(strings).hasSize(1);
        assertThat(strings.get(0)).isEqualTo("a");
        assertThat(response.get().responseHeaders().get("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);
    }

    @Test
    public void shouldSendStreamingResultWithHeaders() {
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/withHeaders").response((resp, body)->{
                response.set(resp);
                return body.asString();
            }).collectList().block();

            assertThat(strings).hasSize(2);
            assertThat(strings.get(0)).isEqualTo("a");
            assertThat(strings.get(1)).isEqualTo("b");
            assertThat(response.get().responseHeaders().get("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);
            assertThat(response.get().responseHeaders().get("my-header")).isEqualTo("my-value");
        } finally {
            server.disposeNow();
        }
    }

    /**
     * Here the implementing class has annotated its method with stream. That should work
     */
    @Path("stream")
    interface StreamingResource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        Observable<String> streamOfStrings();

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("shouldNotStream")
        Observable<String> noStreamOfStrings();

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("withHeaders")
        Observable<String> streamWithHeaders();
    }

    /**
     * This resource has tried to annotate interface with @Stream, should not work
     */
    @Path("nostream")
    interface NoStreamingResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Stream
        Observable<String> noStreamOfStrings();
    }

    public class StreamingResourceImpl implements StreamingResource {
        @Override
        @Stream
        public Observable<String> streamOfStrings() {
            return just("a", "b");
        }

        @Override
        public Observable<String> noStreamOfStrings() {
            return just("a", "b");
        }

        @Override
        @Stream
        public Observable<String> streamWithHeaders() {
            return withHeaders(just("a", "b"), new HashMap<String, String>(){{
                put("my-header", "my-value");
            }});
        }
    }

    public class NoStreamingResourceImpl implements NoStreamingResource {

        @Override
        public Observable<String> noStreamOfStrings() {
            return just("a", "b");
        }
    }
}
