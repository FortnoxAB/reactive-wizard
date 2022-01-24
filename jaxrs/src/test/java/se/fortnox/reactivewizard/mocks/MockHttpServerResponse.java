package se.fortnox.reactivewizard.mocks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.WebsocketServerSpec;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MockHttpServerResponse implements HttpServerResponse {
    private Publisher<? extends byte[]> output;
    private HttpHeaders headers = new DefaultHttpHeaders();
    private HttpResponseStatus status;

    public String getOutp() {
        if (output == null) {
            return "";
        }
        return Flux.from(output)
            .collect(ByteArrayOutputStream::new, this::collectChunks)
            .map(ByteArrayOutputStream::toString)
            .block();
    }

    private void collectChunks(ByteArrayOutputStream outputStream, byte[] bytes) {
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public HttpServerResponse addCookie(Cookie cookie) {
        return null;
    }

    @Override
    public HttpServerResponse addHeader(CharSequence name, CharSequence value) {
        headers.add(name, value);
        return this;
    }

    @Override
    public HttpServerResponse chunkedTransfer(boolean chunked) {
        return null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return null;
    }

    @Override
    public NettyOutbound sendByteArray(Publisher<? extends byte[]> dataStream) {
        if (output == null) {
            output = dataStream;
        }
        return this;
    }

    @Override
    public NettyOutbound sendString(Publisher<? extends String> dataStream) {
        return sendByteArray(Flux.from(dataStream).map(String::getBytes));
    }

    @Override
    public NettyOutbound send(Publisher<? extends ByteBuf> dataStream, Predicate<ByteBuf> predicate) {
        return null;
    }

    @Override
    public NettyOutbound sendObject(Publisher<?> dataStream, Predicate<Object> predicate) {
        return null;
    }

    @Override
    public NettyOutbound sendObject(Object message) {
        return null;
    }

    @Override
    public <S> NettyOutbound sendUsing(Callable<? extends S> sourceInput, BiFunction<? super Connection, ? super S, ?> mappedInput, Consumer<? super S> sourceCleanup) {
        return null;
    }

    @Override
    public HttpServerResponse withConnection(Consumer<? super Connection> withConnection) {
        return null;
    }

    @Override
    public HttpServerResponse compression(boolean compress) {
        return null;
    }

    @Override
    public boolean hasSentHeaders() {
        return false;
    }

    @Override
    public HttpServerResponse header(CharSequence name, CharSequence value) {
        return null;
    }

    @Override
    public HttpServerResponse headers(HttpHeaders headers) {
        return null;
    }

    @Override
    public HttpServerResponse keepAlive(boolean keepAlive) {
        return null;
    }

    @Override
    public HttpHeaders responseHeaders() {
        return headers;
    }

    @Override
    public Mono<Void> send() {
        return null;
    }

    @Override
    public NettyOutbound sendHeaders() {
        return null;
    }

    @Override
    public Mono<Void> sendNotFound() {
        return null;
    }

    @Override
    public Mono<Void> sendRedirect(String location) {
        return null;
    }

    @Override
    public Mono<Void> sendWebsocket(BiFunction<? super WebsocketInbound, ? super WebsocketOutbound, ? extends Publisher<Void>> websocketHandler, WebsocketServerSpec websocketServerSpec) {
        return null;
    }

    @Override
    public HttpServerResponse sse() {
        return null;
    }

    @Override
    public HttpResponseStatus status() {
        return status;
    }

    @Override
    public HttpServerResponse status(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public HttpServerResponse trailerHeaders(Consumer<? super HttpHeaders> consumer) {
        return this;
    }

    @Override
    public Map<CharSequence, Set<Cookie>> cookies() {
        return null;
    }

    @Override
    public boolean isKeepAlive() {
        return false;
    }

    @Override
    public boolean isWebsocket() {
        return false;
    }

    @Override
    public HttpMethod method() {
        return null;
    }

    @Override
    public String fullPath() {
        return null;
    }

    @Override
    public String requestId() {
        return null;
    }

    @Override
    public String uri() {
        return null;
    }

    @Override
    public HttpVersion version() {
        return null;
    }

    @Override
    public Map<CharSequence, List<Cookie>> allCookies() {
        return null;
    }
}
