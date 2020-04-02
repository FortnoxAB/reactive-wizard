package se.fortnox.reactivewizard.reactorclient;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.SslProvider;
import se.fortnox.reactivewizard.client.HttpClientConfig;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class ReactorRxClientProvider {
    private final ConcurrentHashMap<InetSocketAddress, HttpClient> clients = new ConcurrentHashMap<>();
    private final HttpClientConfig                                 config;

    @Inject
    public ReactorRxClientProvider(HttpClientConfig config) {
        this.config = config;
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
        } catch (Throwable e) {
            throw new RuntimeException("Unable to create secure https client.", e);
        }
    }

    private HttpClient buildClient(InetSocketAddress socketAddress) {

        HttpClient client = HttpClient
            .create(ConnectionProvider.fixed("http-connections", config.getMaxConnections()))
            .port(config.getPort())
            .followRedirect(false);

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
