package se.fortnox.reactivewizard.helloworld;

import io.reactivex.netty.protocol.http.server.HttpServer;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;

public class Main {

    public static void main(String[] args) {
        HttpServer.newServer(8080)
            .start(new JaxRsRequestHandler(new HelloWorldResource()))
            .awaitShutdown();
    }
}
