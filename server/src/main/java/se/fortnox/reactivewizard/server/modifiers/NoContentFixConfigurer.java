package se.fortnox.reactivewizard.server.modifiers;

import reactor.netty.http.server.HttpServer;
import se.fortnox.reactivewizard.server.NoContentFixConfigurator;
import se.fortnox.reactivewizard.server.ReactorServerConfigurer;

public class NoContentFixConfigurer implements ReactorServerConfigurer {

    @Override
    public HttpServer configure(HttpServer httpServer) {
        return httpServer.doOnChannelInit((connectionObserver, channel, socketAddress) -> {
            NoContentFixConfigurator noContentFixConfigurator = new NoContentFixConfigurator();
            noContentFixConfigurator.accept(channel.pipeline());
        });
    }
}
