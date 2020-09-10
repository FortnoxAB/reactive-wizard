package se.fortnox.reactivewizard.jaxrs;

import org.reactivestreams.Publisher;
import rx.Observable;

import java.util.Set;

/**
 * Interface that allows for customized JaxRsResource call handling.
 * Applications may register interceptors to add pre- or post processing behaviour common to all resource calls without
 * having to modify the resources.
 *
 * The preHandle and postHandle methods will be run in the same thread but the execution of the call will run
 * asynchronously. Any processing of the actual call should be made on the resourceCall observable in postHandle.
 */
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public interface JaxRsResourceInterceptor {

    /**
     * Intercept the call to a JaxRsResource.
     * Called after the resource has been determined but before the resource has been called.
     * @param context describing the request
     */
    default void preHandle(JaxRsResourceContext context) {
    }

    /**
     * Intercept the call to a JaxRsResource.
     * Called after the resource call. The resource call results in a Publisher that has not yet been
     * subscribed to.
     * @param context describing the request
     * @param resourceCall the processing of the request
     */
    default void postHandle(JaxRsResourceContext context, Publisher<Void> resourceCall) {
    }

    /**
     * The information available to the interceptors.
     */
    interface JaxRsResourceContext {
        /**
         * @return The http method as a String (GET, POST, UPDATE, DELETE...)
         */
        String getHttpMethod();

        /**
         * @return The raw undecoded request uri of the http request including path and request parameters.
         */
        String getRequestUri();

        /**
         * @return The names of all request headers
         */
        Set<String> getRequestHeaderNames();

        /**
         * @param name A request header name
         * @return The value of the request header with the given name
         */
        String getRequestHeader(String name);

        /**
         * @return The full path to the intercepted resource
         */
        String getResourcePath();
    }
}
