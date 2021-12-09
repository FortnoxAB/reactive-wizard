package se.fortnox.reactivewizard.server;

import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import rx.functions.Action0;
import se.fortnox.reactivewizard.RequestHandler;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import static java.util.Arrays.asList;

/**
 * Runs an Reactor @{@link HttpServer} with all registered @{@link RequestHandler}s.
 */
@Singleton
public class RwServer extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(RwServer.class);
    private static final int COMPRESSION_THRESHOLD_BYTES = 1000;
    private static final Set<String> COMPRESSIBLE_MIME_TYPES = new HashSet<>(asList(
        "text/plain",
        "application/xml",
        "text/css",
        "application/x-javascript",
        "application/json"
    ));

    private final ServerConfig config;
    private final ConnectionCounter connectionCounter;
    private final DisposableServer server;
    private static Runnable blockShutdownUntil;

    @Inject
    public RwServer(ServerConfig config, CompositeRequestHandler compositeRequestHandler, ConnectionCounter connectionCounter) {
        this(config, connectionCounter, createHttpServer(config), compositeRequestHandler);
    }

    RwServer(ServerConfig config, ConnectionCounter connectionCounter, HttpServer httpServer,
             CompositeRequestHandler compositeRequestHandler
    ) {
        super("RwServerMain");
        this.config = config;
        this.connectionCounter = connectionCounter;

        if (config.isEnabled()) {
            server = httpServer.handle(compositeRequestHandler).bindNow();
            LOG.info("Server started on port {}", server.port());
            start();
            registerShutdownHook();
        } else {
            server = null;
        }
    }

    private static HttpServer createHttpServer(ServerConfig config) {
        if (!config.isEnabled()) {
            return null;
        }

        return HttpServer
            .create()
            .compress(COMPRESSION_THRESHOLD_BYTES)
            .compress(isCompressionEnabled(config).and(isCompressibleResponse()))
            .port(config.getPort())
            // Register a channel group, when invoking disposeNow() the implementation will wait for the active requests to finish
            .channelGroup(new DefaultChannelGroup(new DefaultEventExecutor()))
            .doOnChannelInit((connectionObserver, channel, socketAddress) -> {
                NoContentFixConfigurator noContentFixConfigurator = new NoContentFixConfigurator();
                noContentFixConfigurator.call(channel.pipeline());
            })
            .httpRequestDecoder(requestDecoderSpec -> requestDecoderSpec
                .maxInitialLineLength(config.getMaxInitialLineLengthDefault())
                .maxHeaderSize(config.getMaxHeaderSize()));
    }

    private static BiPredicate<HttpServerRequest, HttpServerResponse> isCompressibleResponse() {
        return (request, response) -> {
            if (!response.responseHeaders().contains(HttpHeaderNames.CONTENT_LENGTH)) {
                return false;
            }
            return Optional.ofNullable(response.responseHeaders()
                .get(HttpHeaderNames.CONTENT_TYPE))
                .map(HttpUtil::getMimeType)
                .map(CharSequence::toString)
                .map(String::toLowerCase)
                .map(COMPRESSIBLE_MIME_TYPES::contains)
                .orElse(false);
        };
    }

    private static BiPredicate<HttpServerRequest, HttpServerResponse> isCompressionEnabled(ServerConfig config) {
        return (request, response) -> config.isEnableGzip();
    }

    /**
     * Run the thread until server is shutdown.
     */
    @Override
    public void run() {
        server.onDispose().block();
    }

    public DisposableServer getServer() {
        return server;
    }

    void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownHook(config, server, connectionCounter)));
    }

    public static void registerShutdownDependency(Runnable blockShutdownUntil) {
        if (RwServer.blockShutdownUntil != null && blockShutdownUntil != null) {
            throw new IllegalStateException("Shutdown dependency is already registered");
        }
        RwServer.blockShutdownUntil = blockShutdownUntil;
    }

    static void shutdownHook(ServerConfig config, DisposableServer server, ConnectionCounter connectionCounter) {
        LOG.info("Shutdown requested. Waiting {} seconds before commencing.", config.getShutdownDelaySeconds());
        try {
            Thread.sleep(config.getShutdownDelaySeconds() * 1000);
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for shutdown to commence.", e);
            Thread.currentThread().interrupt();
        }
        LOG.info("Shutdown commencing. Will wait up to {} seconds for ongoing requests to complete.", config.getShutdownTimeoutSeconds());
        int elapsedSeconds = measureElapsedSeconds(() ->
            awaitShutdownDependency(config.getShutdownTimeoutSeconds())
        );
        int secondsLeft = Math.max(config.getShutdownTimeoutSeconds() - elapsedSeconds, 0);
        if (!connectionCounter.awaitZero(secondsLeft, TimeUnit.SECONDS)) {
            LOG.error("Shutdown proceeded while connection count was not zero: {}", connectionCounter.getCount());
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
            Thread.currentThread().interrupt();
        }
        LOG.info("Shutdown dependency completed, continue...");
    }

    static int measureElapsedSeconds(Action0 function) {
        Instant start = Instant.now();
        function.call();
        Instant finish = Instant.now();
        return (int) Duration.between(start, finish).getSeconds();
    }
}
