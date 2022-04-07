package se.fortnox.reactivewizard.jaxrs;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import rx.Observable;
import se.fortnox.reactivewizard.CollectionOptions;
import se.fortnox.reactivewizard.db.MockDb;
import se.fortnox.reactivewizard.db.Query;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator.withHeaders;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.testServer;

public class StreamingDataTest {

    private StreamingResource   streamingResource = new StreamingResourceImpl(null, null);
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

        //But at the end of the day
        ByteBufCollector collector = new ByteBufCollector();
        HttpClient httpClient = HttpClient.create().baseUrl("http://localhost:" + server.port());
        assertThat(collector.collectString(httpClient.get().uri("/stream").responseContent()).block()).isEqualTo("ab");
        assertThat(collector.collectString(httpClient.get().uri("/nostream").responseContent()).block()).isEqualTo("a");
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

    @Test
    public void shouldNotSignalLastRecordIfMoreItems() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=5").response((resp, body)->{
                response.set(resp);
                return body.asString();
            }).collectList().block();

            assertThat(strings).hasSize(5);
            assertThat(collectionOptions.isLastRecord()).isFalse();
        } finally {
            server.disposeNow();
        }
    }


    @Test
    public void shouldSignalLastRecordIfNoMoreItems() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=10").response((resp, body)->{
                response.set(resp);
                return body.asString();
            }).collectList().block();

            assertThat(strings).hasSize(10);
            assertThat(collectionOptions.isLastRecord()).isTrue();
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldSignalLastRecordIfLimitIsHigherThanItemCount() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=15").response((resp, body)->{
                response.set(resp);
                return body.asString();
            }).collectList().block();

            assertThat(strings).hasSize(10);
            assertThat(collectionOptions.isLastRecord()).isTrue();
        } finally {
            server.disposeNow();
        }
    }

    @Test
    public void shouldSignalLastRecordIfEmptyResponse() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(0, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=15").response((resp, body)->{
                response.set(resp);
                return body.asString();
            }).collectList().block();

            assertThat(strings).hasSize(0);
            assertThat(collectionOptions.isLastRecord()).isTrue();
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

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("paging")
        Flux<String> streamWithPaging(@QueryParam("limit") String limit);
    }

    interface StreamingDao {
        @Query("SELECT * FROM names WHERE name = :name")
        Flux<String> getData(String name, CollectionOptions collectionOptions);
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

        private StreamingDao streamingDao;
        private final CollectionOptions collectionOptions;

        public StreamingResourceImpl(StreamingDao streamingDao, CollectionOptions collectionOptions) {
            this.streamingDao = streamingDao;
            this.collectionOptions = collectionOptions;
        }

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

        @Override
        public Flux<String> streamWithPaging(String limit) {
            collectionOptions.setLimit(Integer.parseInt(limit));
            return streamingDao.getData("test", collectionOptions).map(s -> {
                return String.valueOf(s);
            });
        }
    }

    public class NoStreamingResourceImpl implements NoStreamingResource {

        @Override
        public Observable<String> noStreamOfStrings() {
            return just("a", "b");
        }
    }
}
