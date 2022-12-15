package se.fortnox.reactivewizard.mocks;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.HttpData;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.Connection;
import reactor.netty.http.Cookies;
import reactor.netty.http.HttpOperations;
import reactor.netty.http.server.HttpServerFormDecoderProvider;
import reactor.netty.http.server.HttpServerRequest;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class MockHttpServerRequest implements HttpServerRequest {
    private final String uri;
    private final HttpMethod method;
    private final ByteBufFlux content;
    private final String path;
    private HttpHeaders headers = new DefaultHttpHeaders();
    private Map<CharSequence, Set<Cookie>> cookies;

    public MockHttpServerRequest(String uri, HttpMethod method, String data) {
        this(uri, method, createByteBufFlux(data));
    }

    public MockHttpServerRequest(String uri, HttpMethod method) {
        this(uri, method, (String)null);
    }

    public MockHttpServerRequest(String uri, HttpMethod method, byte[] data) {
        this(uri, method, ByteBufFlux.fromInbound(Flux.just(data)));
    }

    private static ByteBufFlux createByteBufFlux(String data) {
        if (data == null) {
            return ByteBufFlux.fromString(Flux.empty());
        }
        return ByteBufFlux.fromString(Flux.just(data));
    }

    public MockHttpServerRequest(String uri, HttpMethod method, ByteBufFlux content) {
        this.path = HttpOperations.resolvePath(uri);
        this.uri = uri;
        this.method = method;
        this.content = content;
    }

    public MockHttpServerRequest(String uri) {
        this(uri, HttpMethod.GET, (String)null);
    }

    @Override
    public ByteBufFlux receive() {
        return content;
    }

    @Override
    public Flux<?> receiveObject() {
        return null;
    }

    @Override
    public HttpServerRequest withConnection(Consumer<? super Connection> withConnection) {
        return null;
    }

    @Nullable
    @Override
    public String param(CharSequence key) {
        return null;
    }

    @Nullable
    @Override
    public Map<String, String> params() {
        return null;
    }

    @Override
    public HttpServerRequest paramsResolver(Function<? super String, Map<String, String>> headerResolver) {
        return null;
    }

    @Override
    public boolean isFormUrlencoded() {
        return false;
    }

    @Override
    public boolean isMultipart() {
        return false;
    }

    @Override
    public Flux<HttpData> receiveForm() {
        return null;
    }

    @Override
    public Flux<HttpData> receiveForm(Consumer<HttpServerFormDecoderProvider.Builder> consumer) {
        return null;
    }

    @Override
    public InetSocketAddress hostAddress() {
        return null;
    }

    @Override
    public SocketAddress connectionHostAddress() {
        return null;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return null;
    }

    @Override
    public SocketAddress connectionRemoteAddress() {
        return null;
    }

    @Override
    public HttpHeaders requestHeaders() {
        return headers;
    }

    @Override
    public String scheme() {
        return null;
    }

    @Override
    public String connectionScheme() {
        return null;
    }

    @Override
    public Map<CharSequence, Set<Cookie>> cookies() {
        if (cookies == null) {
            this.cookies = new HashMap<>(Cookies.newServerRequestHolder(requestHeaders(), ServerCookieDecoder.LAX).getCachedCookies());
        }
        return cookies;
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
        return method;
    }

    @Override
    public String fullPath() {
        return path;
    }

    @Override
    public String requestId() {
        return null;
    }

    @Override
    public String uri() {
        return uri;
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
