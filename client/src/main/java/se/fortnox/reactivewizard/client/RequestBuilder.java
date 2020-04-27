package se.fortnox.reactivewizard.client;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static rx.Observable.just;

/**
 * Represents both a client request and a server info (both host and url), which
 * is needed when the same http-proxy should use different hosts for different
 * calls.
 */
public class RequestBuilder {

    private static final Charset             charset = Charset.forName("UTF-8");
    private              InetSocketAddress   serverInfo;
    private              HttpMethod          method;
    private              String              key;
    private              Map<String, String> headers = new HashMap<>();
    private              String              uri;
    private              byte[]              content;

    public RequestBuilder(InetSocketAddress serverInfo, HttpMethod method, String key) {
        this.serverInfo = serverInfo;
        this.method = method;
        this.key = method + " " + key;
    }

    public Observable<HttpClientResponse<ByteBuf>> submit(HttpClient<ByteBuf, ByteBuf> client) {
        HttpClientRequest<ByteBuf, ByteBuf> request = client.createRequest(method, uri);
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request = request.addHeader(header.getKey(), header.getValue());
        }
        if (content != null) {
            request = request.addHeader(CONTENT_LENGTH, content.length);
            return request.writeBytesContent(just(content));
        }
        return request;
    }

    public InetSocketAddress getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(InetSocketAddress serverInfo) {
        this.serverInfo = serverInfo;
    }

    public String getFullUrl() {
        return serverInfo.getHostName() + ":" + serverInfo.getPort() + uri;
    }

    @Override
    public String toString() {
        return method + " " + getFullUrl();
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public boolean canHaveBody() {
        return method.equals(HttpMethod.POST)
            || method.equals(HttpMethod.PUT)
            || method.equals(HttpMethod.DELETE)
            || method.equals(HttpMethod.PATCH);
    }

    public boolean hasContent() {
        return content != null;
    }

    public void setContent(String content) {
        setContent(content.getBytes(charset));
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public HttpMethod getHttpMethod() {
        return method;
    }

    public String getKey() {
        return key;
    }

    public void addQueryParam(String key, String value) {
        String prefix = uri.contains("?") ? "&" : "?";
        uri += prefix + key + "=" + value;
    }

    protected byte[] getContent() {
        return content;
    }
}
