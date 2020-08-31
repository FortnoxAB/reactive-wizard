package se.fortnox.reactivewizard.client;

import reactor.netty.http.client.HttpClientResponse;
import rx.Single;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Internal class to help support getting the full response as return value when Single is returned
 * @param <T> the type of data to be returned
 */
class SingleWithResponse<T> extends Single<T> {

    private final AtomicReference<HttpClientResponse> httpClientResponse;

    SingleWithResponse(Single<T> inner, AtomicReference<HttpClientResponse> httpClientResponse) {
        super((OnSubscribe<T>)inner::subscribe);
        this.httpClientResponse = httpClientResponse;
    }

    HttpClientResponse getResponse() {
        if (httpClientResponse.get() == null) {
            throw new IllegalStateException("This method can only be called after the response has been received");
        }
        return httpClientResponse.get();
    }

    static <T> SingleWithResponse<T> from(SingleWithResponse<T> singleWithResponse, Single<T> inner) {
        return new SingleWithResponse<T>(inner, singleWithResponse.httpClientResponse);
    }
}
