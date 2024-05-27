package se.fortnox.reactivewizard.client;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jakarta.inject.Inject;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.SslProvider;
import se.fortnox.reactivewizard.metrics.HealthRecorder;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * Due to the fact that reactors HttpClient is reusable for multiple connections in parallel.
 * this class is responsible for delivering singleton HttpClients connected to different servers (InetSocketAddresses)
 *
 */
public class ReactorRxClientProvider {
    private final ConcurrentHashMap<InetSocketAddress, HttpClient> clients = new ConcurrentHashMap<>();
    private final HttpClientConfig                                 config;
    private final HealthRecorder                                   healthRecorder;

    @Inject
    public ReactorRxClientProvider(HttpClientConfig config, HealthRecorder healthRecorder) {
        this.config = config;
        this.healthRecorder = healthRecorder;
    }

    public HttpClient clientFor(InetSocketAddress serverInfo) {
        return clients.computeIfAbsent(serverInfo, this::buildClient);
    }

    private HttpClient setupSsl(HttpClient client, boolean isValidateCertificates) {
        if (!isValidateCertificates) {
            return client.secure(this::configureUnsafeSsl);
        }
        try {
            return client.secure(this::configureSsl);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create secure https client.", e);
        }
    }

    private HttpClient buildClient(InetSocketAddress socketAddress) {

        ConnectionProvider connectionProvider = ConnectionProvider
            .builder("http-connections")
            .maxConnections(config.getMaxConnections())
            .pendingAcquireMaxCount(-1)
            .maxIdleTime(Duration.of(config.getConnectionMaxIdleTimeInMs(), ChronoUnit.MILLIS))
            .pendingAcquireTimeout(Duration.ofMillis(config.getPoolAcquireTimeoutMs()))
            .build();

        final AtomicInteger errorCount = new AtomicInteger(0);

        HttpClient client = HttpClient
            .create(connectionProvider)
            .tcpConfiguration(tcpClient -> tcpClient
                .doOnConnected(connection -> {
                    connection.addHandler(new ReadTimeoutHandler(config.getReadTimeoutMs(), TimeUnit.MILLISECONDS));
                })
            )
            .port(config.getPort())
            .doOnRequest((httpClientRequest, connection) -> {
                errorCount.set(0);
                healthRecorder.logStatus(connectionProvider, true);
            })
            .doOnError((httpClientRequest, throwable) -> {
                healthRecorder.logStatus(connectionProvider, errorCount.incrementAndGet() <= config.getNumberOfConnectionFailuresAllowed());
            }, (httpClientResponse, throwable) -> { })
            .followRedirect(config.isFollowRedirect());

        if (config.isHttps()) {
            return setupSsl(client, config.isValidateCertificates());
        }

        return client;
    }

    /**
     * Allows for customization of the ssl context.
     */
    void configureSsl(SslProvider.SslContextSpec spec) {
        spec.sslContext(SslContextBuilder.forClient())
            .defaultConfiguration(SslProvider.DefaultConfigurationType.TCP)
            .handshakeTimeoutMillis(30000);
    }

    /**
     * Allows for customization of the unsafe ssl context.
     */
    void configureUnsafeSsl(SslProvider.SslContextSpec spec) {
        spec.sslContext(SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
            )
            .defaultConfiguration(SslProvider.DefaultConfigurationType.TCP)
            .handshakeTimeoutMillis(30000);
    }
}
