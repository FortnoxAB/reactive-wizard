package se.fortnox.reactivewizard.springserver.configurers;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.stereotype.Service;
import reactor.netty.http.server.HttpServer;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.inject.Inject;

@Service
public class PortConfigurer implements NettyServerCustomizer {

    private final ServerConfig serverConfig;

    @Inject
    public PortConfigurer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public HttpServer apply(HttpServer httpServer) {
        return httpServer.port(serverConfig.getPort());
    }
}
