package se.fortnox.reactivewizard.client;

import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.netty.http.client.HttpClientResponse;

import java.util.Map;

/**
 * Class used when the full response is needed
 * @param <T> the type of data to be returned
 */
public class Response<T> {
    private final T                   body;
    private final HttpResponseStatus  status;
    private final HttpHeaders         headers;


    public Response(HttpClientResponse httpClientResponse, T body) {
        this.status = httpClientResponse.status();
        this.headers = httpClientResponse.responseHeaders();
        this.body = body;
    }

    public T getBody() {
        return body;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    /**
     * Case insensitive fetching a header value
     * @param header the header name
     * @return the value of the header or null if the header is missing
     */
    public String getHeader(String header) {
        return headers.get(header);
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
        return ImmutableMap.copyOf(headers);
    }
}
