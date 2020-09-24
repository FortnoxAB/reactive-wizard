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

import static reactor.netty.channel.BootstrapHandlers.updateConfiguration;

/**
 * Runs an Reactor @{@link HttpServer} with all registered @{@link RequestHandler}s.
 */
@Singleton
public class RwServer extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(RwServer.class);
    private final ServerConfig config;
    private final ConnectionCounter connectionCounter;
    private final LoopResources eventLoopGroup;
    private final DisposableServer server;
    private static Runnable blockShutdownUntil;

    @Inject
    public RwServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter) {
        this(config, compositeRequestHandler, connectionCounter, null);
    }

    RwServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter, LoopResources loopResources) {
        this(config, connectionCounter, createHttpServer(config, loopResources), compositeRequestHandler, loopResources);
    }

    RwServer(ServerConfig config, ConnectionCounter connectionCounter, HttpServer httpServer,
             CompositeRequestHandler compositeRequestHandler, LoopResources loopResources) {
        super("RwServerMain");
        this.config = config;
        this.connectionCounter = connectionCounter;
        this.eventLoopGroup = loopResources;

        if (config.isEnabled()) {
            server = httpServer.handle(compositeRequestHandler).bindNow();
            LOG.info("Server started on port " + server.port());
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
        return HttpServer
            .create()
            .port(config.getPort())
            .tcpConfiguration(tcpServer -> {
                if (loopResources != null) {
                    tcpServer = tcpServer.runOn(loopResources);
                }

                NoContentFixConfigurator noContentFixConfigurator = new NoContentFixConfigurator();
                return tcpServer.doOnBind(serverBootstrap -> updateConfiguration(serverBootstrap, "rw-server-configuration",
                    (connectionObserver, channel) -> {
                        noContentFixConfigurator.call(channel.pipeline());
                    }));
            })
            .httpRequestDecoder(requestDecoderSpec -> requestDecoderSpec
                .maxInitialLineLength(config.getMaxInitialLineLengthDefault())
                .maxHeaderSize(config.getMaxHeaderSize()));
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

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static void registerShutdownDependency(Runnable blockShutdownUntil) {
        if (RwServer.blockShutdownUntil != null && blockShutdownUntil != null) {
            throw new IllegalStateException("Shutdown dependency is already registered");
        }
        RwServer.blockShutdownUntil = blockShutdownUntil;
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
        if (loopResources == null) {
            return;
        }
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
