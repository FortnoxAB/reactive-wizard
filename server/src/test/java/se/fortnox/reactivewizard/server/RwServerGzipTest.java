package se.fortnox.reactivewizard.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class RwServerGzipTest {
    private RwServer rwServer;

    @Test
    public void shouldCompressAllowedContentTypes() {
        Arrays.asList("text/plain", "application/xml", "text/css", "application/x-javascript", "application/json")
            .forEach(allowedContentType -> assertCompressionForContentType(true, allowedContentType, true));
    }

    @Test
    public void shouldCompressWhenMultipleDirectivesAreCombined() {
        Arrays.asList("text/plain; boundary=something", "text/plain; charset=UTF-8", "text/plain; charset=UTF-8; boundary=something")
            .forEach(allowedContentType -> assertCompressionForContentType(true, allowedContentType, true));
    }

    @Test
    public void shouldNotCompressAllowedContentTypeWhenGzipIsDisabled() {
        Arrays.asList("text/plain", "application/xml", "text/css", "application/x-javascript", "application/json")
            .forEach(allowedContentType -> assertCompressionForContentType(false, allowedContentType, false));
    }

    @Test
    public void shouldNotCompressNonAllowedContentTypes() {
        assertCompressionForContentType(true, "application/pdf", false);
    }

    @Test
    public void shouldNotCompressWhenMissingContentType() {
        assertCompressionForContentType(true, null, false);
    }

    @Test
    public void shouldNotCompressSmallPayload() {
        assertCompressionForContentType(true, "text/plain", 5, false);
    }

    @Test
    public void shouldNotCompressIfMissingContentLength() {
        assertCompressionForContentType(true, "text/plain", 5, true, false);
    }

    private RwServer server(RequestHandler handler, boolean compress) {
        ServerConfig config = new ServerConfig();
        config.setPort(0);
        config.setEnableGzip(compress);
        ConnectionCounter connectionCounter = new ConnectionCounter();
        CompositeRequestHandler handlers = new CompositeRequestHandler(Collections.singleton(handler), new ExceptionHandler(new ObjectMapper()), connectionCounter);
        return new RwServer(config, handlers, connectionCounter);
    }

    private void assertCompressionForContentType(boolean serverUsesCompression, String contentType, boolean compressionExpected) {
        assertCompressionForContentType(serverUsesCompression, contentType, 1024, false, compressionExpected);
    }

    private void assertCompressionForContentType(boolean serverUsesCompression, String contentType, Integer contentLength, boolean compressionExpected) {
        assertCompressionForContentType(serverUsesCompression, contentType, contentLength, false, compressionExpected);
    }

    private void assertCompressionForContentType(boolean serverUsesCompression,
                                                 String contentType,
                                                 Integer contentLength,
                                                 boolean chunked,
                                                 boolean compressionExpected
    ) {
        try {
            final String randomContent = randomAlphabetic(contentLength);
            rwServer = server((httpServerRequest, httpServerResponse) -> {
                if (contentType != null) {
                    httpServerResponse = httpServerResponse
                        .addHeader(CONTENT_TYPE, contentType);
                }
                if (!chunked) {
                    httpServerResponse = httpServerResponse
                        .addHeader(CONTENT_LENGTH, String.valueOf(contentLength));
                }
                return httpServerResponse
                    .chunkedTransfer(chunked)
                    .sendByteArray(Mono.just(randomContent.getBytes(UTF_8)));
            }, serverUsesCompression);

            HttpClient.ResponseReceiver<?> responseReceiver = HttpClient.create()
                .baseUrl("http://localhost:" + rwServer.getServer().port())
                .headers(headers -> headers.add("Accept-encoding", "gzip"))
                .get();

            String responseContent = responseReceiver.responseContent()
                .aggregate()
                .asString()
                .block();
            String actualContentType = responseReceiver.response()
                .block()
                .responseHeaders()
                .get(CONTENT_TYPE);

            assertThat(actualContentType)
                .isEqualTo(contentType);
            if (compressionExpected) {
                assertThat(responseContent)
                    .describedAs("Expected compressed content not to be equal to raw content for content type %s", contentType)
                    .isNotEqualTo(randomContent);
            } else {
                assertThat(responseContent)
                    .describedAs("Expected content to be equal to raw content for content type %s", contentType)
                    .isEqualTo(randomContent);
            }
        } finally {
            rwServer.getServer()
                .disposeNow();
        }
    }

    private String randomAlphabetic(Integer contentLength) {
        Random random = new Random(0);
        return Stream.generate(()->String.valueOf((char)random.nextInt('a','z')))
            .limit(contentLength)
            .collect(Collectors.joining());
    }
}
