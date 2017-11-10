package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.response.JaxRsResult;
import se.fortnox.reactivewizard.util.DebugUtil;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles incoming requests. If the request matches a resource an Observable which completes the request is returned.
 */
@Singleton
public class JaxRsRequestHandler implements RequestHandler<ByteBuf, ByteBuf> {

    private JaxRsResources resources;

    private ExceptionHandler exceptionHandler;

    @Inject
    public JaxRsRequestHandler(JaxRsServiceProvider services,
                               JaxRsResourceFactory jaxRsResourceFactory,
                               ExceptionHandler exceptionHandler) {
        this(services.getServices(),
                jaxRsResourceFactory,
                exceptionHandler,
                null);
    }

    public JaxRsRequestHandler(Object... services) {
        this(services, new JaxRsResourceFactory(), new ExceptionHandler(), null);
    }


    public JaxRsRequestHandler(Object[] services,
                               JaxRsResourceFactory jaxRsResourceFactory,
                               ExceptionHandler exceptionHandler,
                               Boolean classReloading) {
        this.exceptionHandler = exceptionHandler;
        if (classReloading == null) {
            classReloading = DebugUtil.IS_DEBUG;
        }
        this.resources = new JaxRsResources(services, jaxRsResourceFactory, classReloading);
    }

    /**
     * Handles incoming request if a matching resource is found.
     *
     * @param request
     * @param response
     * @return an Observable which will complete the request when subsribed, or null if no resource matches the request.
     */
    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        Observable<JaxRsResult<?>> resultObservable;
        try {
            resultObservable = resources.call(request);
        } catch (Exception e) {
            return exceptionHandler.handleException(request, response, e);
        }

        if (resultObservable == null) {
            return null;
        }

        return resultObservable.singleOrDefault(null).flatMap(result -> {
            if (result != null) {
                return result
                        .write(response)
                        .flatMap(done -> response.close());
            }
            return response.close();
        });
    }
}
