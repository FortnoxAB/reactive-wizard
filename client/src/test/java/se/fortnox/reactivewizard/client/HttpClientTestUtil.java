package se.fortnox.reactivewizard.client;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.mockito.Mockito;
import reactor.netty.http.client.HttpClientResponse;
import rx.Observable;
import rx.Single;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    public static <T> Observable<T> mockResponseWithHeadersAndCookies(Observable<T> source, Map<String, String> headers, Map<String, String> cookies, HttpResponseStatus httpResponseStatus) {
        HttpClientResponse httpClientResponse = Mockito.mock(HttpClientResponse.class);

        Map<CharSequence, Set<Cookie>> cookieMap = new HashMap<>();
        cookies.forEach((key, value) -> {
            cookieMap.put(key, Collections.singleton(new DefaultCookie(key, value)));
        });
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        headers.forEach(httpHeaders::add);
        when(httpClientResponse.responseHeaders()).thenReturn(httpHeaders);
        when(httpClientResponse.cookies()).thenReturn(cookieMap);
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

    public static <T> Single<T> mockResponseWithHeadersAndCookies(Single<T> source, Map<String, String> headers, Map<String, String> cookies, HttpResponseStatus httpResponseStatus) {
        HttpClientResponse httpClientResponse = Mockito.mock(HttpClientResponse.class);

        Map<CharSequence, Set<Cookie>> cookieMap = new HashMap<>();
        cookies.forEach((key, value) -> {
            cookieMap.put(key, Collections.singleton(new DefaultCookie(key, value)));
        });
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        headers.forEach(httpHeaders::add);
        when(httpClientResponse.responseHeaders()).thenReturn(httpHeaders);
        when(httpClientResponse.cookies()).thenReturn(cookieMap);
        when(httpClientResponse.status()).thenReturn(httpResponseStatus);


        return new SingleWithResponse<>(source, new AtomicReference<>(httpClientResponse));
    }
}
