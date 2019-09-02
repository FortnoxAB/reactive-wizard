package se.fortnox.reactivewizard.server;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Runs an RxNetty @{@link HttpServer} with all registered @{@link io.reactivex.netty.protocol.http.server.RequestHandler}s.
 */
@Singleton
public class RxNettyServer extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(RxNettyServer.class);
    private final ServerConfig config;
    private final ConnectionCounter connectionCounter;
    public static final int MAX_CHUNK_SIZE_DEFAULT = 8192;
    private final HttpServer<ByteBuf, ByteBuf> server;

    @Inject
    public RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter) {
        this(config, connectionCounter, createHttpServer(config), compositeRequestHandler);
    }

    RxNettyServer(ServerConfig config, ConnectionCounter connectionCounter, HttpServer<ByteBuf, ByteBuf> httpServer,
                  CompositeRequestHandler compositeRequestHandler) {
        super("RxNettyServerMain");
        this.config = config;
        this.connectionCounter = connectionCounter;

        if (config.isEnabled()) {
            server = httpServer.start(compositeRequestHandler);
            start();
            registerShutdownHook();
        } else {
            server = null;
        }
    }

    private static HttpServer<ByteBuf, ByteBuf> createHttpServer(ServerConfig config) {
        if (!config.isEnabled()) {
            return null;
        }
        return HttpServer.newServer(config.getPort())
            .<ByteBuf, ByteBuf>pipelineConfigurator(
                new NoContentFixConfigurator(
                    config.getMaxInitialLineLengthDefault(),
                    MAX_CHUNK_SIZE_DEFAULT,
                    config.getMaxHeaderSize()));
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

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook(config, server, connectionCounter)));
    }

    static void shutdownHook(ServerConfig config, HttpServer server, ConnectionCounter connectionCounter) {
        LOG.info("Shutdown requested. Will wait up to {} seconds...", config.getShutdownTimeoutSeconds());
        server.shutdown();
        if (!connectionCounter.awaitZero(config.getShutdownTimeoutSeconds(), TimeUnit.SECONDS)) {
            LOG.error("Shutdown proceeded while connection count was not zero: " + connectionCounter.getCount());
        }
        server.awaitShutdown();
        LOG.info("Shutdown complete");
    }

}
