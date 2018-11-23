package se.fortnox.reactivewizard.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.logging.LoggingContext;

import javax.inject.Inject;
import java.util.Set;

/**
 * Calls each @{@link RequestHandler} with a request until one returns a result.
 * Delegates to @{@link ExceptionHandler} when any error occurrs.
 */
public class CompositeRequestHandler implements RequestHandler<ByteBuf,ByteBuf> {
    private static final Logger log = LoggerFactory.getLogger(CompositeRequestHandler.class);
    private final Set<RequestHandler<ByteBuf, ByteBuf>> handlers;
    private final ExceptionHandler exceptionHandler;
    private final ConnectionCounter connectionCounter;

    @Inject
    public CompositeRequestHandler(Set<RequestHandler<ByteBuf, ByteBuf>> handlers, ExceptionHandler exceptionHandler, ConnectionCounter connectionCounter) {
        this.handlers = handlers;
        this.exceptionHandler = exceptionHandler;
        this.connectionCounter = connectionCounter;
    }

    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        final long requestStartTime = System.currentTimeMillis();
        try {
            LoggingContext.reset();
            for (RequestHandler<ByteBuf, ByteBuf> handler : handlers) {
                Observable<Void> result = handler.handle(request, response);
                if (result != null) {
                    result = result.onErrorResumeNext(exception -> exceptionHandler
                            .handleException(request, response, exception));
                    return result;
                }
            }
        } catch (Exception exception) {
            return exceptionHandler.handleException(request, response, exception);
        }
        return exceptionHandler.handleException(request,
                response,
                new WebException(HttpResponseStatus.NOT_FOUND))
                .doOnTerminate(() -> RequestLogger.logRequestResponse(request, response, requestStartTime, log))
                .doOnSubscribe(connectionCounter::increase)
                .doOnUnsubscribe(connectionCounter::decrease);
    }
}
