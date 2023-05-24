package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.channel.AbortedException;
import reactor.netty.http.server.HttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;
import se.fortnox.reactivewizard.test.LoggingVerifier;
import se.fortnox.reactivewizard.test.LoggingVerifierExtension;

import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.logging.log4j.Level.DEBUG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(LoggingVerifierExtension.class)
class JaxRsRequestTest {

    LoggingVerifier loggingVerifier = new LoggingVerifier(JaxRsRequest.class, DEBUG);

    @Test
    void shouldDecodeMultiChunkBody() {
        byte[] byteArray = "ö".getBytes(Charset.defaultCharset());
        ByteBufFlux content = ByteBufFlux.fromInbound(Flux.just(new byte[]{byteArray[0]}, new byte[]{byteArray[1]}));
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.POST, content);
        JaxRsRequest req = new JaxRsRequest(serverReq, new ByteBufCollector());
        String body = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo("ö");
    }

    @Test
    void shouldDecodeSingleChunkBody() {
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.POST, "ö");
        JaxRsRequest req = new JaxRsRequest(serverReq, new ByteBufCollector());
        String body = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo("ö");
    }

    @Test
    void shouldDecodeBodyForDeleteRequests() {
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.DELETE, "test");
        JaxRsRequest req = new JaxRsRequest(serverReq, new ByteBufCollector());
        String body = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo("test");
    }

    @Test
    void shouldDecodeBodyOfSpecifiedSize() {
        String input = generateLargeString(5);
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.DELETE, input);
        JaxRsRequest req = new JaxRsRequest(serverReq, new ByteBufCollector(5 * 1024 * 1024));
        String body = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo(input);
    }

    @Test
    void shouldFailWhenDecodingTooLargeBody() {
        String input = generateLargeString(6);
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.DELETE, input);
        JaxRsRequest req = new JaxRsRequest(serverReq, new ByteBufCollector(5 * 1024 * 1024));
        try {
            req.loadBody().block();
            Assertions.fail("Should throw exception");
        } catch (WebException e) {
            assertThat(e.getError()).isEqualTo("too.large.input");
        }
    }

    @Test
    void testParams() {
        MockHttpServerRequest serverReq = new MockHttpServerRequest("/");
        JaxRsRequest req = new JaxRsRequest(serverReq, null, new byte[0], new ByteBufCollector());
        assertThat(req.getPathParam("path")).isNull();
        assertThat(req.getQueryParam("query")).isNull();
        assertThat(req.getFormParam("form")).isNull();
        assertThat(req.getHeader("header")).isNull();
        assertThat(req.getCookie("cookie")).isNull();
        assertThat(req.getCookieValue("test")).isNull();
        assertThat(req.getCookieValue("test", "default")).isEqualTo("default");
    }

    @Test
    void testUri() {
        MockHttpServerRequest serverReq = new MockHttpServerRequest("https://localhost:8080/path?query");
        JaxRsRequest req = new JaxRsRequest(serverReq, null, new byte[0], new ByteBufCollector());
        assertThat(req.getUri()).isEqualTo(serverReq.uri());
    }

    @Test
    void shouldLogOnDebugWhenRequestWasAborted() {
        HttpServerRequest httpRequest = mock(HttpServerRequest.class);
        when(httpRequest.method())
            .thenReturn(HttpMethod.POST);
        when(httpRequest.uri())
            .thenReturn("/path");

        ByteBufFlux byteBufFluxError = ByteBufFlux.fromInbound(Flux.error(new AbortedException("poff")));
        when(httpRequest.receive())
            .thenReturn(byteBufFluxError);

        JaxRsRequest request = new JaxRsRequest(httpRequest);
        request.loadBody()
            .onErrorResume((error) -> Mono.empty())
            .block();

        loggingVerifier
            .verify(DEBUG, "Error reading data for request POST /path");
    }


    private String generateLargeString(int sizeInMB) {
        String largeString = IntStream.range(1, sizeInMB * 1024 * 1024 - 1)
            .mapToObj(i -> "a")
            .collect(Collectors.joining());

        System.out.println(largeString.getBytes().length);

        return "\"" + largeString + "\"";
    }

}
