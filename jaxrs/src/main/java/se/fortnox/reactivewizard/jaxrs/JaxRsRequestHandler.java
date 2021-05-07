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
import java.util.Collections;

/**
 * Handles incoming requests. If the request matches a resource an Observable which completes the request is returned.
 */
@Singleton
public class JaxRsRequestHandler implements RequestHandler {
    private final JaxRsResources   resources;
    private final ExceptionHandler exceptionHandler;
    private final ByteBufCollector          collector;
    private final JaxRsResourceInterceptors requestInterceptors;

    @Inject
    public JaxRsRequestHandler(JaxRsResourcesProvider services,
        JaxRsResourceFactory jaxRsResourceFactory,
        ExceptionHandler exceptionHandler,
        ByteBufCollector collector,
        JaxRsResourceInterceptors requestInterceptors
    ) {
        this(services.getResources(),
            jaxRsResourceFactory,
            exceptionHandler,
            collector,
            null,
            requestInterceptors
        );
    }

    public JaxRsRequestHandler(JaxRsResourcesProvider services,
        JaxRsResourceFactory jaxRsResourceFactory,
        ExceptionHandler exceptionHandler,
        ByteBufCollector collector) {
        this(services.getResources(), jaxRsResourceFactory, exceptionHandler, collector, null,
            new JaxRsResourceInterceptors(Collections.emptySet()));
    }

    public JaxRsRequestHandler(Object[] services,
        JaxRsResourceFactory jaxRsResourceFactory,
        ExceptionHandler exceptionHandler,
        Boolean classReloading) {
        this(services, jaxRsResourceFactory, exceptionHandler, classReloading, new JaxRsResourceInterceptors(Collections.emptySet()));
    }

    public JaxRsRequestHandler(Object[] services,
        JaxRsResourceFactory jaxRsResourceFactory,
        ExceptionHandler exceptionHandler,
        Boolean classReloading,
        JaxRsResourceInterceptors requestInterceptors
    ) {
        this(services, jaxRsResourceFactory, exceptionHandler, new ByteBufCollector(), classReloading, requestInterceptors);
    }

    public JaxRsRequestHandler(Object[] services,
        JaxRsResourceFactory jaxRsResourceFactory,
        ExceptionHandler exceptionHandler,
        ByteBufCollector collector,
        Boolean classReloading,
        JaxRsResourceInterceptors requestInterceptors
    ) {
        this.collector = collector;
        this.exceptionHandler = exceptionHandler;
        if (classReloading == null) {
            classReloading = DebugUtil.IS_DEBUG;
        }
        this.resources = new JaxRsResources(services, jaxRsResourceFactory, classReloading);
        this.requestInterceptors = requestInterceptors;
    }

    /**
     * Handles incoming request if a matching resource is found.
     *
     * @param request The incoming request
     * @param response The response that will be sent
     * @return an Observable which will complete the request when subsribed, or null if no resource matches the request.
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

        Publisher<Void> resourceCall = null;
        JaxRsResourceInterceptor.JaxRsResourceContext resourceContext = new JaxRsResourceCallContext(request, resource);
        try {
            requestInterceptors.preHandle(resourceContext);
            resourceCall = resource.call(jaxRsRequest)
                .flatMap(result -> Mono.from(writeResult(response, result)))
                .onErrorResume(e -> Mono.from(exceptionHandler.handleException(request, response, e)))
                .doAfterTerminate(() -> resource.log(request, response, requestStartTime));
            return resourceCall;
        } finally {
            requestInterceptors.postHandle(resourceContext, resourceCall);
        }
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

    JaxRsResources getResources() {
        return resources;
    }
}
