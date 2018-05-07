package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.junit.Test;
import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator.withHeaders;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.body;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.get;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.testServer;

public class StreamingDataTest {
    private StreamingResource   streamingResource   = new StreamingResourceImpl();
    private NoStreamingResource noStreamingResource = new NoStreamingResourceImpl();

    @Test
    public void testStreamingWithRealServer() {

        HttpServer<ByteBuf, ByteBuf> server   = testServer(streamingResource, noStreamingResource).getServer();
        HttpClient<ByteBuf, ByteBuf> client   = HttpClient.newClient("localhost", server.getServerPort());
        HttpClientResponse<ByteBuf>  response = client.createGet("/stream").toBlocking().single();

        List<String> strings = response.getContent().map(byteBuf -> byteBuf.toString(Charset.defaultCharset()))
            .toList()
            .toBlocking()
            .single();

        assertThat(strings).hasSize(2);
        assertThat(strings.get(0)).isEqualTo("a");
        assertThat(strings.get(1)).isEqualTo("b");
        assertThat(response.getHeader("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);

        //When not streaming the response will finish after first string emission
        response = client.createGet("/nostream").toBlocking().single();

        strings = response.getContent().map(byteBuf -> byteBuf.toString(Charset.defaultCharset()))
            .toList()
            .toBlocking()
            .single();

        assertThat(strings).hasSize(1);
        assertThat(strings.get(0)).isEqualTo("a");
        assertThat(response.getHeader("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);

        //But at the end of the day
        assertThat(body(get(streamingResource, "/stream"))).isEqualTo("ab");
        assertThat(body(get(noStreamingResource, "/nostream"))).isEqualTo("ab");
    }

    @Test
    public void shouldSendStreamingResultWithHeaders() {
        HttpServer<ByteBuf, ByteBuf> server   = testServer(streamingResource).getServer();
        try {
            HttpClient<ByteBuf, ByteBuf> client = HttpClient.newClient("localhost", server.getServerPort());
            HttpClientResponse<ByteBuf> response = client.createGet("/stream/withHeaders").toBlocking().single();

            List<String> strings = response.getContent().map(byteBuf -> byteBuf.toString(Charset.defaultCharset()))
                    .toList()
                    .toBlocking()
                    .single();

            assertThat(strings).hasSize(2);
            assertThat(strings.get(0)).isEqualTo("a");
            assertThat(strings.get(1)).isEqualTo("b");
            assertThat(response.getHeader("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);
            assertThat(response.getHeader("my-header")).isEqualTo("my-value");
        } finally {
            server.shutdown();
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
