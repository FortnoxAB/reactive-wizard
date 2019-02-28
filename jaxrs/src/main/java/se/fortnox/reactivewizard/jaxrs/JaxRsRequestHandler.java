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

/**
 * Handles incoming requests. If the request matches a resource an Observable which completes the request is returned.
 */
@Singleton
public class JaxRsRequestHandler implements RequestHandler<ByteBuf, ByteBuf> {

    private JaxRsResources   resources;
    private ExceptionHandler exceptionHandler;
    private ByteBufCollector collector;

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

    public JaxRsRequestHandler(Object... services) {
        this(services, new JaxRsResourceFactory(), new ExceptionHandler(), new ByteBufCollector(10*1024*1024), null);
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
     * @return an Observable which will complete the request when subsribed, or null if no resource matches the request.
     */
    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        JaxRsRequest     jaxRsRequest = new JaxRsRequest(request, collector);
        JaxRsResource<?> resource     = resources.findResource(jaxRsRequest);

        if (resource == null) {
            return null;
        }

        long requestStartTime = System.currentTimeMillis();

        return resource.call(jaxRsRequest)
            .singleOrDefault(null)
            .flatMap(result -> writeResult(response, result))
            .onErrorResumeNext(e -> exceptionHandler.handleException(request, response, e))
            .doAfterTerminate(() -> resource.log(request, response, requestStartTime));
    }

    private Observable<Void> writeResult(HttpServerResponse<ByteBuf> response, JaxRsResult<?> result) {
        if (result != null) {
            return result.write(response);
        }
        return Observable.empty();
    }
}
