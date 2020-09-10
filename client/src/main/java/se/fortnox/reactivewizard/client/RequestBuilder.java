package se.fortnox.reactivewizard.client;

import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.CONTENT_LENGTH;

/**
 * Represents both a client request and a server info (both host and url), which
 * is needed when the same http-proxy should use different hosts for different
 * calls.
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
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

    public Mono<RwHttpClientResponse> submit(
        reactor.netty.http.client.HttpClient client,
        RequestBuilder requestBuilder) {

        return
            Mono.from(client
                .headers(entries -> {
                    for (Map.Entry<String, String> stringStringEntry : requestBuilder.getHeaders().entrySet()) {
                        entries.set(stringStringEntry.getKey(), stringStringEntry.getValue());
                    }

                    if (requestBuilder.getContent() != null) {
                        entries.set(CONTENT_LENGTH, this.getContent().length);
                    }
                })
                .request(requestBuilder.getHttpMethod())
                .uri(requestBuilder.getFullUrl())
                .send((httpClientRequest, nettyOutbound)
                    -> nettyOutbound.sendByteArray(this.getContent() != null ? Mono.just(this.getContent()) : Mono.empty()))
                .responseConnection((httpClientResponse, connection)
                    -> Mono.just(new RwHttpClientResponse(httpClientResponse, connection.inbound().receive()))));
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
