package se.fortnox.reactivewizard.springserver;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.stereotype.Service;
import reactor.netty.http.server.HttpServer;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.inject.Inject;

@Service
public class MyServerConfig implements NettyServerCustomizer {
    private final ServerConfig serverConfig;

    @Inject
    public MyServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public HttpServer apply(HttpServer httpServer) {
        return httpServer.port(serverConfig.getPort());
    }
}
