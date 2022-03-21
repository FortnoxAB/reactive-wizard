package se.fortnox.reactivewizard.server;

import reactor.netty.http.server.HttpServer;

/**
 * Implement this interface in order to configure the server to your own needs
 */
public interface ReactorServerConfigurer {
    /**
     * Make custom configurations to the httpServer
     * @param httpServer the server to be configured
     * @return the configured server.
     */
    HttpServer configure(HttpServer httpServer);

    /**
     * Returns which prio this configurer has.
     *
     * prio 1 will be executed before prio 2
     *
     */
    default int prio() {
        return 10;
    }
}
