package se.fortnox.reactivewizard.server;

import io.reactivex.netty.protocol.http.server.HttpServer;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Runs an RxNetty @{@link HttpServer} with all registered @{@link io.reactivex.netty.protocol.http.server.RequestHandler}s.
 */
@Singleton
public class RxNettyServer extends Thread {

    private final ServerConfig config;
    private final CompositeRequestHandler compositeRequestHandler;

    @Inject
    public RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler) {
        super("RxNettyServerMain");
        this.config = config;
        this.compositeRequestHandler = compositeRequestHandler;

        if (config.isEnabled()) {
            start();
        }
    }

    /**
     * Runs the HttpServer.
     */
    public void run() {
        HttpServer.newServer(config.getPort())
                .start(compositeRequestHandler)
                .awaitShutdown();
    }
}
