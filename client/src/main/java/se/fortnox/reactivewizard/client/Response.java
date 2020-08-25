package se.fortnox.reactivewizard.client;

import com.google.common.collect.ImmutableMap;
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
    private final Map<String, String> headers;

    public Response(HttpClientResponse httpClientResponse, T body) {
        this.status = httpClientResponse.status();
        this.headers = ImmutableMap.copyOf(httpClientResponse.responseHeaders());
        this.body = body;
    }

    public T getBody() {
        return body;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
