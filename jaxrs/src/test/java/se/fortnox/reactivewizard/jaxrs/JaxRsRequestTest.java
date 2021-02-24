package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.server.HttpServerRequest;
import se.fortnox.reactivewizard.mocks.MockHttpServerRequest;

import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class JaxRsRequestTest {
    @Test
    public void shouldDecodeMultiChunkBody() {
        byte[]                  byteArray = "ö".getBytes(Charset.defaultCharset());
        ByteBufFlux content = ByteBufFlux.fromInbound(Flux.just(new byte[]{byteArray[0]}, new byte[]{byteArray[1]}));
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.POST, content);
        JaxRsRequest               req       = new JaxRsRequest(serverReq, new ByteBufCollector());
        String                     body      = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo("ö");
    }

    @Test
    public void shouldDecodeSingleChunkBody() {
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.POST, "ö");
        JaxRsRequest               req       = new JaxRsRequest(serverReq, new ByteBufCollector());
        String                     body      = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo("ö");
    }

    @Test
    public void shouldDecodeBodyForDeleteRequests() {
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.DELETE, "test");
        JaxRsRequest               req       = new JaxRsRequest(serverReq, new ByteBufCollector());
        String                     body      = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo("test");
    }

    @Test
    public void shouldDecodeBodyOfSpecifiedSize() {
        String input = generateLargeString(5);
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.DELETE, input);
        JaxRsRequest               req       = new JaxRsRequest(serverReq, new ByteBufCollector(5 * 1024 * 1024));
        String                     body      = new String(req.loadBody().block().getBody());
        assertThat(body).isEqualTo(input);
    }

    @Test
    public void shouldFailWhenDecodingTooLargeBody() {
        String input = generateLargeString(6);
        HttpServerRequest serverReq = new MockHttpServerRequest("/", HttpMethod.DELETE, input);
        JaxRsRequest               req       = new JaxRsRequest(serverReq, new ByteBufCollector(5 * 1024 * 1024));
        try {
            req.loadBody().block();
            Assert.fail("Should throw exception");
        } catch (WebException e) {
            assertThat(e.getError()).isEqualTo("too.large.input");
        }
    }

    @Test
    public void testParams() throws Exception {
        MockHttpServerRequest serverReq = new MockHttpServerRequest("/");
        JaxRsRequest          req       = new JaxRsRequest(serverReq, null, new byte[0], new ByteBufCollector());
        assertThat(req.getPathParam("path")).isNull();
        assertThat(req.getQueryParam("query")).isNull();
        assertThat(req.getFormParam("form")).isNull();
        assertThat(req.getHeader("header")).isNull();
        assertThat(req.getCookie("cookie")).isNull();
        assertThat(req.getCookieValue("test")).isNull();
        assertThat(req.getCookieValue("test", "default")).isEqualTo("default");
    }

    @Test
    public void testUri() throws Exception {
        MockHttpServerRequest serverReq = new MockHttpServerRequest("https://localhost:8080/path?query");
        JaxRsRequest          req       = new JaxRsRequest(serverReq, null, new byte[0], new ByteBufCollector());
        assertThat(req.getUri()).isEqualTo(serverReq.uri());
    }


    private String generateLargeString(int sizeInMB) {
        String largeString = IntStream.range(1, sizeInMB * 1024 * 1024 - 1)
            .mapToObj(i -> "a")
            .collect(Collectors.joining());

        System.out.println(largeString.getBytes().length);

        return "\"" + largeString + "\"";
    }

}
