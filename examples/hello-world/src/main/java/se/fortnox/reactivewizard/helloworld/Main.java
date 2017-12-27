package se.fortnox.reactivewizard.helloworld;

import io.reactivex.netty.protocol.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;
import se.fortnox.reactivewizard.config.ConfigReader;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        HelloWorldConfig config = ConfigReader.fromFile("config.yml", HelloWorldConfig.class);

        LOG.info("Starting server on port " + config.getPort());

        HttpServer.newServer(config.getPort())
            .start(new JaxRsRequestHandler(new HelloWorldResource()))
            .awaitShutdown();
    }
}
