package se.fortnox.reactivewizard.server;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.WebException;

import javax.inject.Inject;
import java.util.Set;

/**
 * Calls each @{@link RequestHandler} with a request until one returns a result.
 * Delegates to @{@link ExceptionHandler} when any error occurrs.
 */
public class CompositeRequestHandler implements RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(CompositeRequestHandler.class);
    private final Set<RequestHandler> handlers;
    private final ExceptionHandler exceptionHandler;
    private final ConnectionCounter connectionCounter;
    private final RequestLogger requestLogger;

    @Inject
    public CompositeRequestHandler(Set<RequestHandler> handlers, ExceptionHandler exceptionHandler,
                                   ConnectionCounter connectionCounter, RequestLogger requestLogger) {
        this.handlers = handlers;
        this.exceptionHandler = exceptionHandler;
        this.connectionCounter = connectionCounter;
        this.requestLogger = requestLogger;
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest request, HttpServerResponse response) {
        final long requestStartTime = System.currentTimeMillis();
        try {
            for (RequestHandler handler : handlers) {
                Publisher<Void> result = handler.apply(request, response);
                if (result != null) {

                    return Flux.from(result).onErrorResume(exception -> exceptionHandler
                            .handleException(request, response, exception));
                }
            }
        } catch (Exception exception) {
            return exceptionHandler.handleException(request, response, exception);
        }
        return Flux.from(exceptionHandler.handleException(request,
                response,
                new WebException(HttpResponseStatus.NOT_FOUND)))
                .doOnTerminate(() -> requestLogger.logRequestResponse(request, response, requestStartTime, log))
                .doOnSubscribe(s -> connectionCounter.increase())
                .doFinally(s -> connectionCounter.decrease());
    }
}
