package se.fortnox.reactivewizard.server;

import io.netty.buffer.ByteBuf;
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
    public static final int MAX_CHUNK_SIZE_DEFAULT = 8192;
    private final HttpServer<ByteBuf, ByteBuf> server;

    @Inject
    public RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler) {
        super("RxNettyServerMain");
        this.config = config;
        this.compositeRequestHandler = compositeRequestHandler;

        if (config.isEnabled()) {
            server = HttpServer.newServer(config.getPort())
                    .<ByteBuf,ByteBuf>pipelineConfigurator(
                            new NoContentFixConfigurator(
                                    config.getMaxInitialLineLengthDefault(),
                                    MAX_CHUNK_SIZE_DEFAULT,
                                    config.getMaxHeaderSize()))
                    .start(compositeRequestHandler);

            start();
        } else {
            server = null;
        }
    }

    /**
     * Run the thread until server is shutdown
     */
    public void run() {
        server.awaitShutdown();
    }

    public HttpServer<ByteBuf, ByteBuf> getServer() {
        return server;
    }
}
