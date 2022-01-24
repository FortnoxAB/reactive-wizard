package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import rx.Observable;
import rx.Observer;
import se.fortnox.reactivewizard.util.ReactiveDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Allows you to return response headers together with your result.
 */
public class ResponseDecorator {

    /**
     * Use this to wrap your Observable with some headers. This must be the last decoration before returning from your
     * resource. Any Observable operator applied after this will remove the headers.
     *
     * @param value   is the response
     * @param headers is your headers
     * @param <T>     is the type of value
     * @return the value, decorated with headers.
     */
    @Nonnull
    public static <T> Observable<T> withHeaders(@Nonnull Observable<T> value, @Nonnull Map<String, String> headers) {
        return ResponseDecorator.of(value).withHeaders(headers).build();
    }

    /**
     * Creates a DecoratedResponseBuilder for the supplied Observable.
     * <p>
     * Use {@link DecoratedResponseBuilder#withHeaders} and {@link DecoratedResponseBuilder#withStatus} to decorate
     * your response.
     * Use {@link DecoratedResponseBuilder#build} to create the decorated Observable.
     * <p>
     * This must be the last decoration before returning from your resource.
     * Any Observable operator applied after this will remove the headers.
     * <p>
     * Example:
     * <pre>{@code
     *     ResponseDecorator.of(just("body"))
     *         .withHeaders(Map.of("Location", "/somewhere-else"))
     *         .withStatus(HttpResponseStatus.MOVED_PERMANENTLY)
     *         .build();
     * }</pre>
     *
     * @param value is the response
     * @param <T>   is the type of value
     * @return DecoratedResponseBuilder&lt;T&gt;
     */
    @Nonnull
    public static <T> DecoratedResponseBuilder<T> of(@Nonnull T value) {
        return new DecoratedResponseBuilder<>(value);
    }

    protected static <T> Flux<T> apply(@Nonnull Flux<T> output, @Nonnull JaxRsResult<T> result) {
        Optional<Object> decorations = ReactiveDecorator.getDecoration(output);

        if (decorations.isPresent()) {
            Object object = decorations.get();

            if (object instanceof ResponseDecorations) {
                ResponseDecorations responseDecorations = (ResponseDecorations)object;

                responseDecorations.applyOn(result);

                ApplyDecorationsOnEmit<T> applyDecorations = new ApplyDecorationsOnEmit<>(responseDecorations, result);

                return output
                    .doOnNext(applyDecorations::onNext)
                    .doOnComplete(applyDecorations::onCompleted);
            }
        }
        return output;
    }

    private static class ApplyDecorationsOnEmit<T> implements Observer<T> {

        private final AtomicBoolean hasApplied = new AtomicBoolean();
        private final ResponseDecorations decorations;
        private final JaxRsResult<T> result;

        public ApplyDecorationsOnEmit(@Nonnull ResponseDecorations decorations, @Nonnull JaxRsResult<T> result) {
            this.decorations = decorations;
            this.result = result;
        }

        @Override
        public void onCompleted() {
            applyDecorations();
        }

        @Override
        public void onError(@Nonnull Throwable exception) {
        }

        @Override
        public void onNext(@Nonnull T record) {
            applyDecorations();
        }

        private void applyDecorations() {
            if (hasApplied.compareAndSet(false, true)) {
                decorations.applyOn(result);
            }
        }
    }

    public static class DecoratedResponseBuilder<T> {

        private final T response;
        private final ResponseDecorations decorations = new ResponseDecorations();

        public DecoratedResponseBuilder(T response) {
            this.response = response;
        }

        /**
         * Set the headers map that will be used. Replaces any currently set headers.
         * The content of the map may be altered during the handling of a response.
         *
         * @param headers A map of header values.
         * @return this
         */
        @Nonnull
        public DecoratedResponseBuilder<T> withHeaders(@Nonnull Map<String, String> headers) {
            this.decorations.setHeaders(headers);
            return this;
        }

        /**
         * Appends a header to the current header collection.
         *
         * @param name the name of the header
         * @param value the value of the header
         * @return this
         */
        @Nonnull
        public DecoratedResponseBuilder<T> withHeader(@Nonnull String name, @Nonnull String value) {
            this.decorations.addHeader(name, value);
            return this;
        }

        /**
         * Set the status that will be returned. Calling this method will replace any currently set status.
         *
         * @param status The status object
         * @return this
         */
        @Nonnull
        public DecoratedResponseBuilder<T> withStatus(@Nullable HttpResponseStatus status) {
            this.decorations.setStatus(status);
            return this;
        }

        /**
         * Set the status that will be returned. Calling this method will replace any currently set status.
         * The content of the AtomicReference may be altered during the handling of a response.
         *
         * @param status An AtomicReference to the status object
         * @return this
         */
        @Nonnull
        public DecoratedResponseBuilder<T> withStatus(@Nonnull AtomicReference<HttpResponseStatus> status) {
            this.decorations.setStatus(status);
            return this;
        }

        /**
         * Create the decorated response Observable.
         *
         * @return ObservableWithHeaders&lt;T&gt;
         */
        @Nonnull
        public T build() {
            return ReactiveDecorator.decorated(response, decorations);
        }
    }

    public static class ResponseDecorations {

        private Map<String, String> headers = new HashMap<>();
        private AtomicReference<HttpResponseStatus> status = new AtomicReference<>(null);

        public ResponseDecorations() {
        }

        public void setHeaders(@Nonnull Map<String, String> headers) {
            this.headers = headers;
        }

        public void addHeader(@Nonnull String name, @Nonnull String value) {
            headers.put(name, value);
        }

        public void setStatus(@Nullable HttpResponseStatus status) {
            this.status.set(status);
        }

        public void setStatus(@Nonnull AtomicReference<HttpResponseStatus> status) {
            this.status = status;
        }

        @Nonnull
        public Map<String, String> getHeaders() {
            return headers;
        }

        @Nullable
        public HttpResponseStatus getStatus() {
            return status.get();
        }

        /**
         * Apply this on a result.
         * @param result the result
         */
        public void applyOn(@Nonnull JaxRsResult<?> result) {
            headers.forEach(result::addHeader);

            if (status.get() != null) {
                result.responseStatus = status.get();
            }
        }
    }
}
