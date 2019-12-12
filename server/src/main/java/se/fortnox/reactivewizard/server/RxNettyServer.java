package se.fortnox.reactivewizard.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.reactivex.netty.RxNetty;
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
    private final EventLoopGroup eventLoopGroup;
    private final HttpServer<ByteBuf, ByteBuf> server;
    private static Runnable blockShutdownUntil;

    @Inject
    public RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter) {
        this(config, compositeRequestHandler, connectionCounter, RxNetty.getRxEventLoopProvider().globalServerEventLoop(true));
    }

    RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter, EventLoopGroup eventLoopGroup) {
        this(config, connectionCounter, createHttpServer(config, eventLoopGroup), compositeRequestHandler, eventLoopGroup);
    }

    RxNettyServer(ServerConfig config, ConnectionCounter connectionCounter, HttpServer<ByteBuf, ByteBuf> httpServer,
                  CompositeRequestHandler compositeRequestHandler, EventLoopGroup eventLoopGroup) {
        super("RxNettyServerMain");
        this.config = config;
        this.connectionCounter = connectionCounter;
        this.eventLoopGroup = eventLoopGroup;

        if (config.isEnabled()) {
            server = httpServer.start(compositeRequestHandler);
            start();
            registerShutdownHook();
        } else {
            server = null;
        }
    }

    private static HttpServer<ByteBuf, ByteBuf> createHttpServer(ServerConfig config, EventLoopGroup eventLoopGroup) {
        if (!config.isEnabled()) {
            return null;
        }
        return HttpServer.newServer(config.getPort(), eventLoopGroup, NioServerSocketChannel.class)
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook(config, server, eventLoopGroup, connectionCounter)));
    }

    public static void registerShutdownDependency(Runnable blockShutdownUntil) {
        if (RxNettyServer.blockShutdownUntil != null && blockShutdownUntil != null) {
            throw new IllegalStateException("Shutdown dependency is already registered");
        }
        RxNettyServer.blockShutdownUntil = blockShutdownUntil;
    }

    static void shutdownHook(ServerConfig config, HttpServer server, EventLoopGroup eventLoopGroup, ConnectionCounter connectionCounter) {
        LOG.info("Shutdown requested.");
        awaitShutdownDependency();
        LOG.info("Will wait up to {} seconds...", config.getShutdownTimeoutSeconds());
        shutdownEventLoopGracefully(config, eventLoopGroup);
        if (!connectionCounter.awaitZero(config.getShutdownTimeoutSeconds(), TimeUnit.SECONDS)) {
            LOG.error("Shutdown proceeded while connection count was not zero: " + connectionCounter.getCount());
        }
        server.awaitShutdown();
        LOG.info("Shutdown complete");
    }

    static void awaitShutdownDependency() {
        if (blockShutdownUntil == null) {
            return;
        }

        LOG.info("Wait for completion of shutdown dependency");
        Thread thread = new Thread(blockShutdownUntil);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            LOG.error("Fail while waiting shutdown dependency", e);
        }
        LOG.info("Shutdown dependency completed, continue...");
    }

    static void shutdownEventLoopGracefully(ServerConfig config, EventLoopGroup eventLoopGroup) {
        try {
            int shutdownQuietPeriodSeconds = 0;
            eventLoopGroup.shutdownGracefully(shutdownQuietPeriodSeconds, config.getShutdownTimeoutSeconds(), TimeUnit.SECONDS).await();
        } catch (InterruptedException e) {
            LOG.error("Graceful shutdown failed" + e);
        }
    }
}
