package se.fortnox.reactivewizard.server;

import reactor.netty.http.server.HttpServer;

public interface ReactorServerConfigurer {
    HttpServer configure(HttpServer httpServer);
}
