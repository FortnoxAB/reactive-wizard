package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static rx.Observable.just;

/**
 * Represents an incoming request. Helps with extracting different types of data from the request.
 */
public class JaxRsRequest {
    private static final Logger           LOG       = LoggerFactory.getLogger(JaxRsResource.class);
    private static final ByteBufCollector collector = new ByteBufCollector(10 * 1024 * 1024); // 10 MB
    private final HttpServerRequest<ByteBuf> req;
    private final byte[]                     body;
    private       Matcher                    matcher;

    protected JaxRsRequest(HttpServerRequest<ByteBuf> req, Matcher matcher, byte[] body) {
        this.req = req;
        this.matcher = matcher;
        this.body = body;
    }

    public JaxRsRequest(HttpServerRequest<ByteBuf> request) {
        this(request, null, null);
    }

    protected JaxRsRequest create(HttpServerRequest<ByteBuf> req, Matcher matcher, byte[] body) {
        return new JaxRsRequest(req, matcher, body);
    }

    public boolean hasMethod(HttpMethod httpMethod) {
        return req.getHttpMethod().equals(httpMethod);
    }

    public byte[] getBody() {
        return body;
    }

    public Observable<JaxRsRequest> loadBody() {
        HttpMethod httpMethod = req.getHttpMethod();
        if (HttpMethod.POST.equals(httpMethod) || HttpMethod.PUT.equals(httpMethod) ||
            HttpMethod.PATCH.equals(httpMethod) || HttpMethod.DELETE.equals(httpMethod)) {
            return collector.collectBytes(req.getContent()
                .doOnError(e -> LOG.error(
                    "Error reading data for request "
                        + httpMethod + " " + req.getUri(),
                    e)))
                .lastOrDefault(null)
                .map(body -> create(req, matcher, body));
        }
        return just(this);
    }

    public String getQueryParam(String key) {
        return getQueryParam(key, null);
    }

    public String getQueryParam(String key, String defaultValue) {
        List<String> list = req.getQueryParameters().get(key);
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
        return req.getHeader(key, defaultValue);
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
        return req.getCookies().get(key);
    }

    public String getCookieValue(String key) {
        return getCookieValue(key, null);
    }

    public String getCookieValue(String key, String defaultValue) {
        Set<Cookie> cookies = req.getCookies().get(key);
        if (cookies == null || cookies.isEmpty()) {
            return defaultValue;
        }
        return cookies.iterator().next().value();
    }

    public String getPath() {
        return req.getDecodedPath();
    }

    public boolean matchesPath(Pattern pathPattern) {
        matcher = pathPattern.matcher(getPath());
        return matcher.matches();
    }
}
