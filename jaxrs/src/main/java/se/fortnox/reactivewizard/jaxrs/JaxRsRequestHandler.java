package se.fortnox.reactivewizard.jaxrs;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.util.DebugUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.util.Collections.emptySet;

/**
 * Handles incoming requests. If the request matches a resource an Observable which completes the request is returned.
 */
@Singleton
public class JaxRsRequestHandler implements RequestHandler<ByteBuf, ByteBuf> {
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
            requestInterceptors);
    }

    /**
     * A bit dangerous to have a constructor with varargs.
     * It is only used in test but be aware when adding new parameters to the other constructors that
     * Code using the other constructors will end up here if we don't provide overloaded constructors.
     * @param services a list of services to deploy
     */
    public JaxRsRequestHandler(Object... services) {
        this(services, new JaxRsResourceFactory(), new ExceptionHandler(), new ByteBufCollector(), null,
            new JaxRsResourceInterceptors(emptySet()));
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
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        JaxRsRequest     jaxRsRequest = new JaxRsRequest(request, collector);
        JaxRsResource<?> resource     = resources.findResource(jaxRsRequest);

        if (resource == null) {
            return null;
        }

        preHandle(request, resource);

        long requestStartTime = System.currentTimeMillis();

        Observable<Void> resourceCall = null;
        JaxRsResourceInterceptor.JaxRsResourceContext resourceContext = new JaxRsResourceCallContext(request, resource);
        try {
            requestInterceptors.preHandle(resourceContext);
            resourceCall = resource.call(jaxRsRequest)
                .singleOrDefault(null)
                .flatMap(result -> writeResult(response, result))
                .onErrorResumeNext(e -> exceptionHandler.handleException(request, response, e))
                .doAfterTerminate(() -> resource.log(request, response, requestStartTime));
            return resourceCall;
        } finally {
            requestInterceptors.postHandle(resourceContext, resourceCall);
        }
    }

    /**
     * Pre handling hook for classes extending this class
     *
     * @param request the current request
     * @param resource the resource that will we handling the request
     */
    protected void preHandle(HttpServerRequest<ByteBuf> request, JaxRsResource<?> resource) {
    }

    private Observable<Void> writeResult(HttpServerResponse<ByteBuf> response, JaxRsResult<?> result) {
        if (result != null) {
            return result.write(response);
        }
        return Observable.empty();
    }
}
