package se.fortnox.reactivewizard.reactorclient;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import se.fortnox.reactivewizard.jaxrs.WebException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class ByteBufferCollector {

    private final int maxReqSize;

    public ByteBufferCollector() {
        this.maxReqSize = 10485760;
    }

    public ByteBufferCollector(int maxReqSize) {
        this.maxReqSize = maxReqSize;
    }

    public Mono<String> collectString(ByteBufFlux input) {
        return input.collect(ByteArrayOutputStream::new, this::collectChunks)
            .map(this::decodeBody);
    }

    private String decodeBody(ByteArrayOutputStream buf) {
        try {
            return buf.toString(Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException var3) {
            throw new WebException(HttpResponseStatus.BAD_REQUEST, "unsupported.encoding");
        }
    }

    private void collectChunks(ByteArrayOutputStream buf, ByteBuf bytes) {
        try {
            int length = bytes.readableBytes();
            if (buf.size() + length > this.maxReqSize) {
                throw new WebException(HttpResponseStatus.BAD_REQUEST, "too.large.input");
            }

            bytes.readBytes(buf, length);
        } catch (IOException var7) {
            throw new WebException(HttpResponseStatus.BAD_REQUEST, var7);
        }
    }

    public Mono<byte[]> collectBytes(ByteBufFlux input) {
        return input.collect(ByteArrayOutputStream::new, this::collectChunks)
            .map(ByteArrayOutputStream::toByteArray);
    }
}
