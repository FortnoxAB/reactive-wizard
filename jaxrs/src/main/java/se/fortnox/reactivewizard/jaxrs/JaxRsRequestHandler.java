package se.fortnox.reactivewizard.jaxrs;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.util.DebugUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles incoming requests. If the request matches a resource a Flux or Mono which completes the request is returned.
 */
@Singleton
public class JaxRsRequestHandler implements RequestHandler {
    private final JaxRsResources   resources;
    private final ExceptionHandler exceptionHandler;
    private final ByteBufCollector          collector;

    @Inject
    public JaxRsRequestHandler(JaxRsResourcesProvider services,
                               JaxRsResourceFactory jaxRsResourceFactory,
                               ExceptionHandler exceptionHandler,
                               ByteBufCollector collector
    ) {
        this(services.getResources(),
            jaxRsResourceFactory,
            exceptionHandler,
            collector,
            null);
    }

    public JaxRsRequestHandler(Object[] services,
        JaxRsResourceFactory jaxRsResourceFactory,
        ExceptionHandler exceptionHandler,
        Boolean classReloading
    ) {
        this(services, jaxRsResourceFactory, exceptionHandler, new ByteBufCollector(), classReloading);
    }

    public JaxRsRequestHandler(Object[] services,
        JaxRsResourceFactory jaxRsResourceFactory,
        ExceptionHandler exceptionHandler,
        ByteBufCollector collector,
        Boolean classReloading
    ) {
        this.collector = collector;
        this.exceptionHandler = exceptionHandler;
        if (classReloading == null) {
            classReloading = DebugUtil.IS_DEBUG;
        }
        this.resources = new JaxRsResources(services, jaxRsResourceFactory, classReloading);
    }

    /**
     * Handles incoming request if a matching resource is found.
     *
     * @param request The incoming request
     * @param response The response that will be sent
     * @return a Flux/Mono which will complete the request when the Flux/Mono completes or errors, or null if no resource matches the request.
     */
    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        JaxRsRequest     jaxRsRequest = new JaxRsRequest(request, collector);
        JaxRsResource<?> resource     = resources.findResource(jaxRsRequest);

        if (resource == null) {
            return null;
        }

        preHandle(request, resource);

        long requestStartTime = System.currentTimeMillis();

        Publisher<Void> resourceCall;

        resourceCall = resource.call(jaxRsRequest)
            .flatMap(result -> Mono.from(writeResult(response, result)))
            .onErrorResume(e -> Mono.from(exceptionHandler.handleException(request, response, e)))
            .doAfterTerminate(() -> resource.log(request, response, requestStartTime));
        return resourceCall;

    }

    /**
     * Pre handling hook for classes extending this class.
     *
     * @param request the current request
     * @param resource the resource that will we handling the request
     */
    protected void preHandle(HttpServerRequest request, JaxRsResource<?> resource) {
    }

    private Publisher<Void> writeResult(HttpServerResponse response, JaxRsResult<?> result) {
        if (result != null) {
            return result.write(response);
        }
        return Mono.empty();
    }
}
