package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.MockHttpServerRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.UnicastContentSubject;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

public class JaxRsRequestTest {
    @Test
    public void shouldDecodeMultiChunkBody() {
        UnicastContentSubject<ByteBuf> content = UnicastContentSubject.create(1000, TimeUnit.MILLISECONDS);
        byte[] b = "ö".getBytes(Charset.defaultCharset());
        content.onNext(Unpooled.wrappedBuffer(new byte[]{b[0]}));
        content.onNext(Unpooled.wrappedBuffer(new byte[]{b[1]}));
        content.onCompleted();
        HttpServerRequest<ByteBuf> serverReq = new MockHttpServerRequest("/", content);
        JaxRsRequest req = new JaxRsRequest(serverReq);
        String body = req.loadBody().toBlocking().single().getBody();
        assertThat(body).isEqualTo("ö");
    }

    @Test
    public void shouldDecodeSingleChunkBody() {
        UnicastContentSubject<ByteBuf> content = UnicastContentSubject.create(1000, TimeUnit.MILLISECONDS);
        byte[] b = "ö".getBytes(Charset.defaultCharset());
        content.onNext(Unpooled.wrappedBuffer(b));
        content.onCompleted();
        HttpServerRequest<ByteBuf> serverReq = new MockHttpServerRequest("/", content);
        JaxRsRequest req = new JaxRsRequest(serverReq);
        String body = req.loadBody().toBlocking().single().getBody();
        assertThat(body).isEqualTo("ö");
    }

    @Test
    public void shouldDecodeBodyForDeleteRequests() {
        HttpServerRequest<ByteBuf> serverReq = new MockHttpServerRequest("/",
                HttpMethod.DELETE, "test");
        JaxRsRequest req = new JaxRsRequest(serverReq);
        String body = req.loadBody().toBlocking().single().getBody();
        assertThat(body).isEqualTo("test");
    }
}
