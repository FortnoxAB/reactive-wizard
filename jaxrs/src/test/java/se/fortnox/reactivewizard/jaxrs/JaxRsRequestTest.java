package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.MockHttpServerRequest;
import org.junit.Test;
import rx.subjects.UnicastSubject;

import java.nio.charset.Charset;

import static org.fest.assertions.Assertions.assertThat;

public class JaxRsRequestTest {
    @Test
    public void shouldDecodeMultiChunkBody() {
        UnicastSubject<ByteBuf> content = UnicastSubject.create();
        byte[] b = "ö".getBytes(Charset.defaultCharset());
        content.onNext(Unpooled.wrappedBuffer(new byte[]{b[0]}));
        content.onNext(Unpooled.wrappedBuffer(new byte[]{b[1]}));
        content.onCompleted();
        HttpServerRequest<ByteBuf> serverReq = new MockHttpServerRequest("/", HttpMethod.POST, content);
        JaxRsRequest req = new JaxRsRequest(serverReq);
        String body = req.loadBody().toBlocking().single().getBody();
        assertThat(body).isEqualTo("ö");
    }

    @Test
    public void shouldDecodeSingleChunkBody() {
        HttpServerRequest<ByteBuf> serverReq = new MockHttpServerRequest("/", HttpMethod.POST, "ö");
        JaxRsRequest req = new JaxRsRequest(serverReq);
        String body = req.loadBody().toBlocking().single().getBody();
        assertThat(body).isEqualTo("ö");
    }

    @Test
    public void shouldDecodeBodyForDeleteRequests() {
        HttpServerRequest<ByteBuf> serverReq = new MockHttpServerRequest("/", HttpMethod.DELETE, "test");
        JaxRsRequest req = new JaxRsRequest(serverReq);
        String body = req.loadBody().toBlocking().single().getBody();
        assertThat(body).isEqualTo("test");
    }

    @Test
    public void testParams() throws Exception {
        MockHttpServerRequest serverReq = new MockHttpServerRequest("/");
        JaxRsRequest req = new JaxRsRequest(serverReq, null, "");
        assertThat(req.getPathParam("path")).isNull();
        assertThat(req.getQueryParam("query")).isNull();
        assertThat(req.getFormParam("form")).isNull();
        assertThat(req.getHeader("header")).isNull();
        assertThat(req.getCookie("cookie")).isNull();
    }
}
