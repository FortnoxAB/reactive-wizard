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

    private static final Logger logger = LoggerFactory.getLogger(RxNettyServer.class);
    private final ServerConfig config;
    private final CompositeRequestHandler compositeRequestHandler;
    private final ConnectionCounter connectionCounter;
    public static final int MAX_CHUNK_SIZE_DEFAULT = 8192;
    private final HttpServer<ByteBuf, ByteBuf> server;

    @Inject
    public RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter) {
        super("RxNettyServerMain");
        this.config = config;
        this.compositeRequestHandler = compositeRequestHandler;
        this.connectionCounter = connectionCounter;

        if (config.isEnabled()) {
            server = HttpServer.newServer(config.getPort())
                    .<ByteBuf,ByteBuf>pipelineConfigurator(
                            new NoContentFixConfigurator(
                                    config.getMaxInitialLineLengthDefault(),
                                    MAX_CHUNK_SIZE_DEFAULT,
                                    config.getMaxHeaderSize()))
                    .start(compositeRequestHandler);

            start();
            registerShutdownHook();
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

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown requested. Will wait up to 120 seconds...");
            server.shutdown();
            if (!connectionCounter.awaitZero(120, TimeUnit.SECONDS)) {
                logger.error("Shutdown proceeded while connection count was not zero: " + connectionCounter.getCount());
            }
            server.awaitShutdown();
            logger.info("Shutdown complete");
        }));
    }

}
