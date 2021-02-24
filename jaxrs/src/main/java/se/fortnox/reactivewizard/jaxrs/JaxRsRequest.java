package se.fortnox.reactivewizard.jaxrs;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpMethod.PUT;
import static io.netty.handler.codec.http.HttpMethod.DELETE;

/**
 * Represents an incoming request. Helps with extracting different types of data from the request.
 */
public class JaxRsRequest {
    private static final Logger LOG = LoggerFactory.getLogger(JaxRsResource.class);

    private final HttpServerRequest   req;
    private final byte[]              body;
    private final String              path;
    private final String              uri;
    private       Matcher             matcher;
    private final ByteBufCollector    collector;
    private Map<String, List<String>> queryParameters;

    protected JaxRsRequest(HttpServerRequest req, Matcher matcher, byte[] body, ByteBufCollector collector) {
        this.req       = req;
        this.matcher   = matcher;
        this.body      = body;
        this.collector = collector; // 10 MB as default
        this.uri       = req.uri();
        try {
            this.path = req.fullPath();
        } catch (IllegalArgumentException e) {
            throw new WebException(HttpResponseStatus.BAD_REQUEST);
        }
    }

    public JaxRsRequest(HttpServerRequest request) {
        this(request, new ByteBufCollector());
    }

    public JaxRsRequest(HttpServerRequest request, ByteBufCollector collector) {
        this(request, null, null, collector);
    }

    protected JaxRsRequest create(HttpServerRequest req, Matcher matcher, byte[] body, ByteBufCollector collector) {
        return new JaxRsRequest(req, matcher, body, collector);
    }

    public boolean hasMethod(HttpMethod httpMethod) {
        return req.method().equals(httpMethod);
    }

    public byte[] getBody() {
        return body;
    }

    public Mono<JaxRsRequest> loadBody() {
        HttpMethod httpMethod = req.method();
        if (POST.equals(httpMethod) || PUT.equals(httpMethod) || PATCH.equals(httpMethod) || DELETE.equals(httpMethod)) {
            return collector.collectBytes(req.receive()
                .doOnError(e -> LOG.error("Error reading data for request " + httpMethod + " " + req.uri(), e)))
                .defaultIfEmpty(new byte[0])
                .map(reqBody -> create(req, matcher, reqBody, collector));
        }
        return Mono.just(this);
    }

    public String getQueryParam(String key) {
        return getQueryParam(key, null);
    }

    public String getQueryParam(String key, String defaultValue) {
        if (queryParameters == null) {
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
            queryParameters = queryStringDecoder.parameters();
        }
        List<String> list = queryParameters.get(key);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return defaultValue;
    }

    public String getPathParam(String key) {
        return getPathParam(key, null);
    }

    public String getPathParam(String key, String defaultValue) {
        if (matcher == null || !matcher.pattern().pattern().contains(key)) {
            return defaultValue;
        }
        return matcher.group(key);
    }

    public String getHeader(String key) {
        return getHeader(key, null);
    }

    public String getHeader(String key, String defaultValue) {
        return req.requestHeaders().get(key, defaultValue);
    }

    public String getFormParam(String key) {
        return getFormParam(key, null);
    }

    public String getFormParam(String key, String defaultValue) {
        return parseUrlEncodedBody(body, key, defaultValue);
    }

    private String parseUrlEncodedBody(byte[] body, String key, String defaultValue) {
        QueryStringDecoder        decoder    = new QueryStringDecoder(new String(body), false);
        Map<String, List<String>> parameters = decoder.parameters();
        List<String>              list       = parameters.get(key);
        return list == null || list.isEmpty() ? defaultValue : list.get(0);
    }

    public Set<Cookie> getCookie(String key) {
        return req.cookies().get(key);
    }

    public String getCookieValue(String key) {
        return getCookieValue(key, null);
    }

    public String getCookieValue(String key, String defaultValue) {
        Set<Cookie> cookies = req.cookies().get(key);
        if (cookies == null || cookies.isEmpty()) {
            return defaultValue;
        }
        return cookies.iterator().next().value();
    }

    public String getPath() {
        return path;
    }

    public String getUri() { return uri; }

    public boolean matchesPath(Pattern pathPattern) {
        matcher = pathPattern.matcher(getPath());
        return matcher.matches();
    }
}
