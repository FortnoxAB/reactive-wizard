package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.metrics.HealthRecorder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Flux.empty;

public class HttpClientStreamingTest {
    private static final Random RANDOM = new Random();

    private final HealthRecorder healthRecorder = new HealthRecorder();

    private TestResource getHttpProxy(int port) {
        return getHttpProxy(port, Duration.ofSeconds(10));
    }

    private TestResource getHttpProxy(int port, Duration maxRequestTime) {
        return getHttpProxy(port, 1, maxRequestTime);
    }

    private TestResource getHttpProxy(int port, int maxConn, Duration maxRequestTime) {
        try {
            HttpClientConfig config = new HttpClientConfig("localhost:" + port);
            config.setMaxConnections(maxConn);
            config.setReadTimeoutMs((int) maxRequestTime.toMillis());
            return getHttpProxy(config);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private TestResource getHttpProxy(HttpClientConfig config) {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        RequestLogger requestLogger = new RequestLogger();
        HttpClient client = new HttpClient(config,
            new ReactorRxClientProvider(config, healthRecorder),
            mapper,
            new RequestParameterSerializers(),
            Collections.emptySet(),
            requestLogger);
        return client.create(TestResource.class);
    }

    @Test
    public void shouldSupportSendingStreamAsChunkedTransfer() {
        AtomicReference<HttpServerRequest> recordedRequest = new AtomicReference<>();
        AtomicReference<String> recordedRequestBody = new AtomicReference<>("");

        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            recordedRequest.set(request);
            response.status(HttpResponseStatus.NO_CONTENT);
            return request.receive().flatMap(buf -> {
                recordedRequestBody.updateAndGet(value -> value.concat(buf.toString(Charset.defaultCharset())));
                return Flux.empty();
            });
        }).bindNow();

        TestResource resource = getHttpProxy(server.port());

        Flux<byte[]> body = Flux.just("Chu", "nke", "d te", "st", "b", "ody")
            .map(str -> str.getBytes(Charset.defaultCharset()));

        resource.sendChunked(body).block();
        assertThat(recordedRequestBody.get()).isEqualTo("Chunked testbody");
        assertThat(recordedRequest.get().requestHeaders().get("Transfer-Encoding")).isEqualTo("chunked");

        server.disposeNow();
    }

    private Flux<byte[]> generateFile(long bytes) {
        AtomicLong generatedBytes = new AtomicLong(0);
        return Flux.generate(sink -> {
            int length = RANDOM.nextInt(1000, 8000);
            byte[] randomBytes = new byte[length];
            RANDOM.nextBytes(randomBytes);
            generatedBytes.addAndGet(length);
            sink.next(randomBytes);
            if (generatedBytes.get() > bytes) {
                sink.complete();
            }
        });
    }

    @Test
    public void shouldSupportStreamingResponseToRequest() {
        var bytesDownloaded = new AtomicLong(0);
        var bytesUploaded = new AtomicLong(0);
        Flux<? extends byte[]> file = generateFile(1024L * 1024 * 1024);

        DisposableServer server = HttpServer.create().port(0).handle((request, response) -> {
            if (request.method().equals(HttpMethod.GET)) {
                response.status(HttpResponseStatus.OK);
                return response.sendByteArray(file.doOnNext(
                    bytes -> bytesDownloaded.addAndGet(bytes.length)
                )).then();
            } else if (request.method().equals(HttpMethod.POST)) {
                response.status(HttpResponseStatus.OK);
                return request.receive()
                    .map(ByteBuf::readableBytes)
                    .reduce(0L, Long::sum)
                    .doOnSuccess(bytesUploaded::set)
                    .then();
            } else {
                response.status(HttpResponseStatus.NOT_FOUND);
                return empty();
            }
        }).bindNow();

        TestResource testResource = getHttpProxy(server.port(), 2, Duration.ofMinutes(10));
        HttpClient.setTimeout(testResource, 10, ChronoUnit.MINUTES);

        testResource.sendChunked(testResource.getByteStream()).block();

        assertThat(bytesUploaded.get()).isEqualTo(bytesDownloaded.get());

        server.dispose();
    }

    @Path("/")
    public interface TestResource {
        @GET
        @Produces("application/octet-stream")
        @Path("get")
        Flux<byte[]> getByteStream();

        @POST
        @Consumes("application/octet-stream")
        @Path("post")
        Mono<Void> sendChunked(Flux<byte[]> content);
    }
}
