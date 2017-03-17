package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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
    private static final Logger LOG = LoggerFactory
            .getLogger(JaxRsResource.class);
    private static final int MAX_REQUEST_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Matcher matcher;
    private final HttpServerRequest<ByteBuf> req;
    private final String body;

    protected JaxRsRequest(HttpServerRequest<ByteBuf> req, Matcher matcher, String body) {
        this.req = req;
        this.matcher = matcher;
        this.body = body;
    }

    protected JaxRsRequest create(HttpServerRequest<ByteBuf> req, Matcher matcher, String body) {
        return new JaxRsRequest(req, matcher, body);
    }

    public JaxRsRequest(HttpServerRequest<ByteBuf> request) {
        this(request, null, null);
    }

    public boolean hasMethod(HttpMethod httpMethod) {
        return req.getHttpMethod().equals(httpMethod);
    }

    public JaxRsRequest forPath(Pattern path) {
        return create(req, path.matcher(req.getPath()), null);
    }

    public boolean isMatchingPath() {
        return matcher != null && matcher.matches();
    }

    public String getBody() {
        return body;
    }

    public Observable<JaxRsRequest> loadBody() {
        if (HttpMethod.POST.equals(req.getHttpMethod()) || HttpMethod.PUT.equals(req.getHttpMethod()) ||
                HttpMethod.PATCH.equals(req.getHttpMethod()) || HttpMethod.DELETE.equals(req.getHttpMethod())) {
            return req.getContent()
                    .doOnError(e -> LOG.error(
                            "Error reading data for request "
                                    + req.getHttpMethod() + " " + req.getUri(),
                            e))
                    .collect(ByteArrayOutputStream::new, (buf, bytes) -> collectChunks(buf, bytes))
                    .map(buf -> decodeBody(buf))
                    .lastOrDefault(null)
                    .map(body-> create(req, matcher, body));
        }
        return just(this);
    }

    private String decodeBody(ByteArrayOutputStream buf) {
        try {
            return buf.toString(Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
            LOG.error("Unsupported encoding for request "
                    + req.getHttpMethod() + " " + req.getUri(), e);
            return null;
        }
    }

    private void collectChunks(ByteArrayOutputStream buf, ByteBuf bytes) {
        try {
            int length = bytes.readableBytes();
            if (buf.size() + length > MAX_REQUEST_SIZE) {
                throw new WebException(HttpResponseStatus.BAD_REQUEST, "too.large.input");
            } else {
                bytes.readBytes(buf, length);
            }
        } catch (IOException e) {
            LOG.error("Error reading data for request %s %s: %s", req.getHttpMethod(), req.getUri(), e.getMessage());
            throw new WebException(HttpResponseStatus.BAD_REQUEST);
        }
    }

    public String getQueryParam(String key) {
        List<String> list = req.getQueryParameters().get(key);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    public String getPathParam(String key) {
        if (matcher == null || !matcher.pattern().pattern().contains(key)) {
            return null;
        }
        return matcher.group(key);
    }

    public String getHeader(String key) {
        return req.getHeaders().get(key);
    }

    public String getFormParam(String key) {
        return parseUrlEncodedBody(body, key);
    }

    private String parseUrlEncodedBody(String body, String key) {
        QueryStringDecoder decoder = new QueryStringDecoder(body, false);
        Map<String, List<String>> parameters = decoder.parameters();
        List<String> list = parameters.get(key);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public Set<Cookie> getCookie(String key) {
        return req.getCookies().get(key);
    }

    public String getCookieValue(String key) {
        Set<Cookie> cookies = req.getCookies().get(key);
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        return cookies.iterator().next().getValue();
    }

    public String getPath() {
        return req.getPath();
    }
}
