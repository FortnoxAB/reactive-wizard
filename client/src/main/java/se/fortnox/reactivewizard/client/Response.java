package se.fortnox.reactivewizard.client;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.netty.http.client.HttpClientResponse;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * Class used when the full response is needed
 * @param <T> the type of data to be returned
 */
public class Response<T> {
    private final T                  body;
    private final HttpClientResponse httpClientResponse;

    public Response(HttpClientResponse httpClientResponse, T body) {
        this.httpClientResponse = httpClientResponse;
        this.body = body;
    }

    public T getBody() {
        return body;
    }

    public HttpResponseStatus getStatus() {
        return httpClientResponse.status();
    }

    <S> Response<S> withBody(S body) {
        return new Response<>(httpClientResponse, body);
    }

    <S> Response<S> withNoBody() {
        return new Response<>(httpClientResponse, null);
    }

    /**
     * Case insensitive fetching a header value
     * @param header the header name
     * @return the value of the header or null if the header is missing
     */
    public String getHeader(String header) {
        return httpClientResponse
            .responseHeaders()
            .get(header);
    }

    /**
     * Fetching a cookie value
     * @param cookieName the cookie name
     * @return the value(s) of the cookie
     */
    public List<String> getCookie(String cookieName) {

        if (httpClientResponse.cookies().containsKey(cookieName)) {
            return httpClientResponse
                .cookies()
                .get(cookieName)
                .stream()
                .map(Cookie::value)
                .collect(toList());
        }

        return emptyList();

    }

    /**
     * The complete header structure.
     * Note that the keys, the header names, are case sensitive as in any java map.
     * Need case insensitivity?
     * @see Response#getHeader(String header) the case insensitive way of fetching a header.
     *
     *
     *
     * @return a map containing headers mapped to values.
     */
    public Map<String, String> getHeaders() {
        return ImmutableMap.copyOf(httpClientResponse.responseHeaders());
    }
}
