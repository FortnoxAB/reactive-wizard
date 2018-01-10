package se.fortnox.reactivewizard.server;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import se.fortnox.reactivewizard.ExceptionHandler;
import se.fortnox.reactivewizard.jaxrs.RequestLogger;
import se.fortnox.reactivewizard.jaxrs.WebException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Set;

@Singleton
public class ReactiveWizardServer implements RequestHandler<ByteBuf, ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(ReactiveWizardServer.class);
    public static final int MAX_CHUNK_SIZE_DEFAULT = 8192;

    private HttpServer<ByteBuf, ByteBuf> server;

    private Set<RequestHandler> handlers;
    private ExceptionHandler exceptionHandler;

    @Inject
    public ReactiveWizardServer(ServerConfig config,
                                Set<RequestHandler> handlers,
                                ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        if (config.isEnabled()) {
            this.handlers = handlers;
            this.server = HttpServer.newServer(config.getPort())
                    .pipelineConfigurator(
                            new CustomHttpServerPipelineConfigurator(
                                    config.getMaxInitialLineLengthDefault(),
                                    MAX_CHUNK_SIZE_DEFAULT,
                                    config.getMaxHeaderSize()));

            new Thread(this::startAndWait).start();
        }
    }

    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request,
                                   HttpServerResponse<ByteBuf> response) {
        final long requestStartTime = System.currentTimeMillis();
        //LoggingContext.reset();
        for (RequestHandler<ByteBuf, ByteBuf> h : handlers) {
            Observable<Void> r = h.handle(request, response);
            if (r != null) {
                r = r.onErrorResumeNext(e -> exceptionHandler
                        .handleException(request, response, e));
                return r;
            }
        }
        return exceptionHandler.handleException(request,
                response,
                new WebException(HttpResponseStatus.NOT_FOUND))
                .doOnTerminate(() -> RequestLogger.logRequestResponse(request, response, requestStartTime, log));
    }

    public void start() {
        if (server != null) {
            server.start(this);
        }
    }

    public void setHandlers(Set<RequestHandler> handlers) {
        this.handlers = handlers;
    }

    public void shutdown() throws InterruptedException {
        if (server != null) {
            server.shutdown();
            server.awaitShutdown();
            server = null;
        }
    }

    public void startAndWait() {
        if (server != null) {
            this.start();
            server.awaitShutdown();
        }
    }

    public int getServerPort() {
        return server.getServerPort();
    }
}

