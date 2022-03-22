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
     * Configurer with prio 1 will be executed before prio 2
     * which means that configurations executed in a configurer with prio2 could override configurations in prio1.
     *
     * Set a high number if the configuration is really important.
     *
     */
    default int prio() {
        return 10;
    }
}
