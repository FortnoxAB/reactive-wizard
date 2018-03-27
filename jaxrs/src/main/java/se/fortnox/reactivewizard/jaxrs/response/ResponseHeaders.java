package se.fortnox.reactivewizard.jaxrs.response;

import rx.Observable;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;

import java.util.Map;

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

    public static <T> Observable<T> apply(Observable<T> output, JaxRsResult<T> result) {
        if (output instanceof ObservableWithHeaders) {
            Map<String,String> headers = ((ObservableWithHeaders) output).getHeaders();
            headers.forEach(result::addHeader);
        }
        return output;
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
