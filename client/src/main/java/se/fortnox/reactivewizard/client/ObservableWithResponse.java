package se.fortnox.reactivewizard.client;

import reactor.netty.http.client.HttpClientResponse;
import rx.Observable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal class to help support getting the full response as return value when observable is returned
 * @param <T> the type of data to be returned
 */
class ObservableWithResponse<T> extends Observable<T> {

    private final AtomicReference<HttpClientResponse> httpClientResponse;

    ObservableWithResponse(Observable<T> inner, AtomicReference<HttpClientResponse> httpClientResponse) {
        super(inner::unsafeSubscribe);
        this.httpClientResponse = httpClientResponse;
    }

    HttpClientResponse getResponse() {
        if (httpClientResponse.get() == null) {
            throw new IllegalStateException("This method can only be called after the response has been received");
        }
        return httpClientResponse.get();
    }

    static <T> ObservableWithResponse<T> from(ObservableWithResponse observableWithResponse, Observable<T> inner) {
        return new ObservableWithResponse<T>(inner, observableWithResponse.httpClientResponse);
    }
}
