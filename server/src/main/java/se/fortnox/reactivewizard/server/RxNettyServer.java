package se.fortnox.reactivewizard.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.LoopResources;
import rx.functions.Action0;
import se.fortnox.reactivewizard.RequestHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Runs an RxNetty @{@link HttpServer} with all registered @{@link RequestHandler}s.
 */
@Singleton
public class RxNettyServer extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(RxNettyServer.class);
    private final ServerConfig config;
    private final ConnectionCounter connectionCounter;
    public static final int MAX_CHUNK_SIZE_DEFAULT = 8192;
    private final LoopResources eventLoopGroup;
    private final DisposableServer server;
    private static Runnable blockShutdownUntil;

    @Inject
    public RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter) {
        this(config, compositeRequestHandler, connectionCounter, null);
    }

    RxNettyServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter, LoopResources loopResources) {
        this(config, connectionCounter, createHttpServer(config, loopResources), compositeRequestHandler, loopResources);
    }

    RxNettyServer(ServerConfig config, ConnectionCounter connectionCounter, HttpServer httpServer,
                  CompositeRequestHandler compositeRequestHandler, LoopResources loopResources) {
        super("RxNettyServerMain");
        this.config = config;
        this.connectionCounter = connectionCounter;
        this.eventLoopGroup = loopResources;

        if (config.isEnabled()) {
            server = httpServer.handle(compositeRequestHandler).bindNow();
            start();
            registerShutdownHook();
        } else {
            server = null;
        }
    }

    private static HttpServer createHttpServer(ServerConfig config, LoopResources loopResources) {
        if (!config.isEnabled()) {
            return null;
        }
        return HttpServer.create().host("localhost").port(config.getPort());
    }

    /**
     * Run the thread until server is shutdown
     */
    @Override
    public void run() {
        server.onDispose().block();
    }

    public DisposableServer getServer() {
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

    static void shutdownHook(ServerConfig config, DisposableServer server, LoopResources loopResources, ConnectionCounter connectionCounter) {
        LOG.info("Shutdown requested. Will wait up to {} seconds...", config.getShutdownTimeoutSeconds());
        int elapsedSeconds = measureElapsedSeconds(() ->
            awaitShutdownDependency(config.getShutdownTimeoutSeconds())
        );
        int secondsLeft = Math.max(config.getShutdownTimeoutSeconds() - elapsedSeconds, 0);
        shutdownEventLoopGracefully(secondsLeft, loopResources);
        if (!connectionCounter.awaitZero(secondsLeft, TimeUnit.SECONDS)) {
            LOG.error("Shutdown proceeded while connection count was not zero: " + connectionCounter.getCount());
        }
        server.disposeNow(Duration.ofSeconds(config.getShutdownTimeoutSeconds()));
        LOG.info("Shutdown complete");
    }

    static void awaitShutdownDependency(int shutdownTimeoutSeconds) {
        if (blockShutdownUntil == null) {
            return;
        }

        LOG.info("Wait for completion of shutdown dependency");
        Thread thread = new Thread(blockShutdownUntil);
        thread.start();
        try {
            thread.join(Duration.ofSeconds(shutdownTimeoutSeconds).toMillis());
        } catch (InterruptedException e) {
            LOG.error("Fail while waiting shutdown dependency", e);
        }
        LOG.info("Shutdown dependency completed, continue...");
    }

    static void shutdownEventLoopGracefully(int shutdownTimeoutSeconds, LoopResources loopResources) {
        try {
            int shutdownQuietPeriodSeconds = 0;
            loopResources.disposeLater(Duration.ofSeconds(shutdownQuietPeriodSeconds), Duration.ofSeconds(shutdownTimeoutSeconds)).block();
        } catch (Exception e) {
            LOG.error("Graceful shutdown failed" + e);
        }
    }

    static int measureElapsedSeconds(Action0 function) {
        Instant start = Instant.now();
        function.call();
        Instant finish = Instant.now();
        return (int) Duration.between(start, finish).getSeconds();
    }
}
