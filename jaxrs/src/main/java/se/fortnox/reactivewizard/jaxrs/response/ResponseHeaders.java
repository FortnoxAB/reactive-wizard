package se.fortnox.reactivewizard.jaxrs.response;

import rx.Observable;
import rx.Observer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allows you to return response headers together with your result.
 */
public class ResponseHeaders {

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
    public static <T> Observable<T> apply(Observable<T> output, JaxRsResult<T> result) {
        if (output instanceof ObservableWithHeaders) {
            Map<String,String> headers = ((ObservableWithHeaders) output).getHeaders();
            return output.doOnEach(new SetHeadersOnEmit(headers, result));
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
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(T t) {
            setHeaders();
        }

        private void setHeaders() {
            if (headersSet.compareAndSet(false, true)) {
                headers.forEach(result::addHeader);
            }

        }
    }

    private static class ObservableWithHeaders<T> extends Observable<T> {

        private final Map<String, String> headers;

        protected ObservableWithHeaders(Observable<T> inner, Map<String,String> headers) {
            super(inner::unsafeSubscribe);
            this.headers = headers;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }
    }
}
