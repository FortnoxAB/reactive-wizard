package se.fortnox.reactivewizard.client;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.mockito.Mockito;
import reactor.netty.http.client.HttpClientResponse;
import rx.Observable;
import rx.Single;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.when;

/**
 * Util class for mocking response from server with headers
 */
public class HttpClientTestUtil {

    private HttpClientTestUtil() {}

    public static <T> Observable<T> mockResponseWithHeaders(Observable<T> source, Map<String, String> headers, HttpResponseStatus httpResponseStatus) {
        HttpClientResponse httpClientResponse = Mockito.mock(HttpClientResponse.class);

        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        headers.forEach(httpHeaders::add);
        when(httpClientResponse.responseHeaders()).thenReturn(httpHeaders);
        when(httpClientResponse.status()).thenReturn(httpResponseStatus);

        return new ObservableWithResponse<>(source, new AtomicReference<>(httpClientResponse));
    }

    public static <T> Single<T> mockResponseWithHeaders(Single<T> source, Map<String, String> headers, HttpResponseStatus httpResponseStatus) {
        HttpClientResponse httpClientResponse = Mockito.mock(HttpClientResponse.class);

        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        headers.forEach(httpHeaders::add);
        when(httpClientResponse.responseHeaders()).thenReturn(httpHeaders);
        when(httpClientResponse.status()).thenReturn(httpResponseStatus);

        return new SingleWithResponse<>(source, new AtomicReference<>(httpClientResponse));
    }
}
