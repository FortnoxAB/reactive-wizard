package se.fortnox.reactivewizard.server.modifiers;

import com.google.inject.Inject;
import reactor.netty.http.server.HttpServer;
import se.fortnox.reactivewizard.server.ServerConfig;
import se.fortnox.reactivewizard.server.ReactorServerConfigurer;

public class RequestSizesConfigurer implements ReactorServerConfigurer {
    private final ServerConfig serverConfig;

    @Inject
    public RequestSizesConfigurer(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public HttpServer configure(HttpServer httpServer) {
        return httpServer.httpRequestDecoder(requestDecoderSpec -> requestDecoderSpec
            .maxInitialLineLength(serverConfig.getMaxInitialLineLengthDefault())
            .maxHeaderSize(serverConfig.getMaxHeaderSize()));
    }
}
