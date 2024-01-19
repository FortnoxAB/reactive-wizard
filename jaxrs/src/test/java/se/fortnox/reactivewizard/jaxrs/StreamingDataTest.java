package se.fortnox.reactivewizard.jaxrs;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
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
import static se.fortnox.reactivewizard.jaxrs.response.ResponseDecorator.withHeaders;
import static se.fortnox.reactivewizard.utils.JaxRsTestUtil.testServer;

class StreamingDataTest {

    private final StreamingResource   streamingResource = new StreamingResourceImpl(null, null);
    private final NoStreamingResource noStreamingResource = new NoStreamingResourceImpl();

    @Test
    void testStreamingWithRealServer() {

        DisposableServer      server   = testServer(streamingResource, noStreamingResource).getServer();
        HttpClient            client   = HttpClient.create().port(server.port());
        final AtomicReference<HttpClientResponse>    response = new AtomicReference<>();
        List<String> strings = client.get().uri("/stream").response((resp, body) -> {
            response.set(resp);
            return body.asString();
        }).collectList().block();

        assertThat(strings).hasSize(2);
        assertThat(strings.getFirst()).isEqualTo("a");
        assertThat(strings.get(1)).isEqualTo("b");
        assertThat(response.get().responseHeaders().get("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(response.get().responseHeaders().get("Transfer-Encoding")).isEqualTo("chunked");

        //When not streaming the response will finish after first string emission
        strings = client.get().uri("/nostream").response((resp, body) -> {
            response.set(resp);
            return body.asString();
        }).collectList().block();

        assertThat(strings).hasSize(1);
        assertThat(strings.getFirst()).isEqualTo("ab");
        assertThat(response.get().responseHeaders().get("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(response.get().responseHeaders().get("Transfer-Encoding")).isNotEqualTo("chunked");

        //But at the end of the day
        ByteBufCollector collector = new ByteBufCollector();
        HttpClient httpClient = HttpClient.create().baseUrl("http://localhost:" + server.port());
        assertThat(collector.collectString(httpClient.get().uri("/stream").responseContent()).block()).isEqualTo("ab");
        assertThat(collector.collectString(httpClient.get().uri("/nostream").responseContent()).block()).isEqualTo("ab");
    }

    @Test
    void shouldSendStreamingResultWithHeaders() {
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/withHeaders").response((resp, body) -> {
                response.set(resp);
                return body.asString();
            }).collectList().block();

            assertThat(strings).hasSize(2);
            assertThat(strings.getFirst()).isEqualTo("a");
            assertThat(strings.get(1)).isEqualTo("b");
            assertThat(response.get().responseHeaders().get("Content-Type")).isEqualTo(MediaType.TEXT_PLAIN);
            assertThat(response.get().responseHeaders().get("my-header")).isEqualTo("my-value");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void shouldNotSignalLastRecordIfMoreItems() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=5").response((resp, body) -> {
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
    void shouldSignalLastRecordIfNoMoreItems() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=10").response((resp, body) -> {
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
    void shouldSignalLastRecordIfLimitIsHigherThanItemCount() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=15").response((resp, body) -> {
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
    void shouldStreamJsonArrayOfObjects() throws SQLException {
        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);
        CollectionOptions collectionOptions = new CollectionOptions();

        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();

        try {
            HttpClient client = HttpClient.create().port(server.port());

            String content = client.get().uri("/stream/my-entities").responseContent().aggregate().asString().block();
            assertThat(content).isEqualTo("[{\"value\":\"Hello\"},{\"value\":\"World\"}]");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void shouldHaveChunkedTransferEncoding() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);
        CollectionOptions collectionOptions = new CollectionOptions();

        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();

        try {
            HttpClient client = HttpClient.create().port(server.port());

            String content = client.get().uri("/stream/my-entities").response()
                .map(httpClientResponse -> httpClientResponse.responseHeaders().get("Transfer-Encoding"))
                .block();

            assertThat(content).isEqualTo("chunked");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void shouldStreamJsonArrayOfStrings() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(10, StreamingDao.class);
        CollectionOptions collectionOptions = new CollectionOptions();

        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();

        try {
            HttpClient client = HttpClient.create().port(server.port());

            String content = client.get().uri("/stream/json-strings").responseContent().aggregate().asString().block();
            assertThat(content).isEqualTo("[\"Hello\",\"World\"]");
        } finally {
            server.disposeNow();
        }
    }

    @Test
    void shouldSignalLastRecordIfEmptyResponse() throws SQLException {

        MockDb mockDb = new MockDb();
        StreamingDao streamingDao = mockDb.mockDao(0, StreamingDao.class);

        CollectionOptions collectionOptions = new CollectionOptions();
        StreamingResource streamingResource = new StreamingResourceImpl(streamingDao, collectionOptions);
        DisposableServer server   = testServer(streamingResource).getServer();
        try {
            HttpClient client = HttpClient.create().port(server.port());
            final AtomicReference<HttpClientResponse> response = new AtomicReference<>();
            List<String> strings = client.get().uri("/stream/paging?limit=15").response((resp, body) -> {
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
        Flux<String> streamOfStrings();

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("shouldNotStream")
        Flux<String> noStreamOfStrings();

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("withHeaders")
        Flux<String> streamWithHeaders();

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @Path("paging")
        Flux<String> streamWithPaging(@QueryParam("limit") String limit);

        @GET
        @Path("/my-entities")
        Flux<MyEntity> getMyEntities();

        @GET
        @Path("/json-strings")
        Flux<String> getJsonStrings();

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
        Flux<String> noStreamOfStrings();
    }

    public class StreamingResourceImpl implements StreamingResource {

        private final StreamingDao streamingDao;
        private final CollectionOptions collectionOptions;

        public StreamingResourceImpl(StreamingDao streamingDao, CollectionOptions collectionOptions) {
            this.streamingDao = streamingDao;
            this.collectionOptions = collectionOptions;
        }

        @Override
        @Stream
        public Flux<String> streamOfStrings() {
            return Flux.just("a", "b");
        }

        @Override
        public Flux<String> noStreamOfStrings() {
            return Flux.just("a", "b");
        }

        @Override
        @Stream
        public Flux<String> streamWithHeaders() {
            return withHeaders(Flux.just("a", "b"), new HashMap<>(){{
                put("my-header", "my-value");
            }});
        }

        @Override
        @Stream
        public Flux<String> streamWithPaging(String limit) {
            collectionOptions.setLimit(Integer.parseInt(limit));
            return streamingDao.getData("test", collectionOptions).map(String::valueOf);
        }

        @Override
        @Stream
        public Flux<MyEntity> getMyEntities() {
            return Flux.just(new MyEntity("Hello"), new MyEntity("World"));
        }

        @Override
        @Stream
        public Flux<String> getJsonStrings() {
            return Flux.just("Hello", "World");
        }
    }

    public class NoStreamingResourceImpl implements NoStreamingResource {

        @Override
        public Flux<String> noStreamOfStrings() {
            return Flux.just("a", "b");
        }
    }

    public class MyEntity {
        public String value;

        public MyEntity() {
        }

        public MyEntity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setString(String value) {
            this.value = value;
        }
    }
}
