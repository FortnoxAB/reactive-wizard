package se.fortnox.reactivewizard.jaxrs.response;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
     * Use this to wrap your Mono with some headers. This must be the last decoration before returning from your
     * resource. Any Mono operator applied after this will remove the headers.
     *
     * @param value   is the response
     * @param headers is your headers
     * @param <T>     is the type of value
     * @return the value, decorated with headers.
     */
    @Nonnull
    public static <T> Mono<T> withHeaders(@Nonnull Mono<T> value, @Nonnull Map<String, String> headers) {
        return ResponseDecorator.of(value).withHeaders(headers).build();
    }

    /**
     * Use this to wrap your Flux with some headers. This must be the last decoration before returning from your
     * resource. Any Flux operator applied after this will remove the headers.
     *
     * @param value   is the response
     * @param headers is your headers
     * @param <T>     is the type of value
     * @return the value, decorated with headers.
     */
    @Nonnull
    public static <T> Flux<T> withHeaders(@Nonnull Flux<T> value, @Nonnull Map<String, String> headers) {
        return ResponseDecorator.of(value).withHeaders(headers).build();
    }

    /**
     * Creates a DecoratedResponseBuilder for the supplied Flux/Mono.
     * <p>
     * Use {@link DecoratedResponseBuilder#withHeaders} and {@link DecoratedResponseBuilder#withStatus} to decorate
     * your response.
     * Use {@link DecoratedResponseBuilder#build} to create the decorated Flux/Mono.
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
    public static <T extends Publisher<?>> DecoratedResponseBuilder<T> of(@Nonnull T value) {
        return new DecoratedResponseBuilder<>(value);
    }

    protected static <T> Flux<T> apply(@Nonnull Flux<T> output, @Nonnull JaxRsResult<T> result) {
        Optional<Object> decorations = ReactiveDecorator.getDecoration(output);

        if (decorations.isPresent()) {
            Object object = decorations.get();

            if (object instanceof ResponseDecorations responseDecorations) {
                responseDecorations.applyOn(result);

                DecorationsApplier<T> applyDecorations = new DecorationsApplier<>(responseDecorations, result);

                return output
                    .doOnNext(applyDecorations::onNext)
                    .doOnComplete(applyDecorations::onComplete);
            }
        }
        return output;

    }

    private static class DecorationsApplier<T> {

        private final AtomicBoolean hasApplied = new AtomicBoolean();
        private final ResponseDecorations decorations;
        private final JaxRsResult<T> result;

        public DecorationsApplier(@Nonnull ResponseDecorations decorations, @Nonnull JaxRsResult<T> result) {
            this.decorations = decorations;
            this.result = result;
        }

        public void onComplete() {
            applyDecorations();
        }

        public void onNext(@Nonnull T unusedParameter) {
            applyDecorations();
        }

        private void applyDecorations() {
            if (hasApplied.compareAndSet(false, true)) {
                decorations.applyOn(result);
            }
        }
    }

    public static class DecoratedResponseBuilder<T extends Publisher<?>> {

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
         * Create the decorated response Flux/Mono
         *
         * @return decorated response
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
