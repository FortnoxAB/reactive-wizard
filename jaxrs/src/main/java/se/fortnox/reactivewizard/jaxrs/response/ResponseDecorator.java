package se.fortnox.reactivewizard.jaxrs.response;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import rx.Observable;
import rx.Observer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows you to return response headers together with your result.
 */
public class ResponseDecorator {

    /**
     * Use this to wrap your Observable with some headers. This must be the last decoration before returning from your
     * resource. Any Observable operator applied after this will remove the headers.
     * @param value is the response
     * @param headers is your headers
     * @param <T> is the type of value
     * @return the value, decorated with headers.
     */
    public static <T> Observable<T> withHeaders(Observable<T> value, Map<String, String> headers) {
        return new ObservableWithHeaders<>(value, headers);
    }

    @SuppressWarnings("unchecked")
    protected static <T> Flux<T> apply(Flux<T> output, JaxRsResult<T> result) {
        if (output instanceof FluxWithHeaders) {
            Map<String,String> headers = ((FluxWithHeaders) output).getHeaders();
            headers.forEach(result::addHeader);
            SetHeadersOnEmit setHeaders = new SetHeadersOnEmit(headers, result);
            return output
                .doOnNext(setHeaders::onNext)
                .doOnComplete(setHeaders::onCompleted);
        }
        return output;
    }

    private static class SetHeadersOnEmit<T>  implements Observer<T> {

        private final AtomicBoolean headersSet = new AtomicBoolean();
        private final Map<String, String> headers;
        private final JaxRsResult<T> result;

        public SetHeadersOnEmit(Map<String, String> headers, JaxRsResult<T> result) {
            this.headers = headers;
            this.result = result;
        }

        @Override
        public void onCompleted() {
            setHeaders();
        }

        @Override
        public void onError(Throwable exception) {

        }

        @Override
        public void onNext(T record) {
            setHeaders();
        }

        private void setHeaders() {
            if (headersSet.compareAndSet(false, true)) {
                headers.forEach(result::addHeader);
            }

        }
    }

    public static class ObservableWithHeaders<T> extends Observable<T> {

        private final Map<String, String> headers;

        protected ObservableWithHeaders(Observable<T> inner, Map<String,String> headers) {
            super(inner::unsafeSubscribe);
            this.headers = headers;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }


    public static class FluxWithHeaders<T> extends Flux<T> {

        private final Flux<T> inner;
        private final Map<String, String> headers;

        public FluxWithHeaders(Flux<T> inner, Map<String,String> headers) {
            this.inner = inner;
            this.headers = headers;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        @Override
        public void subscribe(CoreSubscriber<? super T> actual) {
            inner.subscribe(actual);
        }
    }
}
