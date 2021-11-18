package se.fortnox.reactivewizard.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Component;
import reactor.netty.http.server.HttpServer;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.config.ConfigFactory;

import javax.inject.Inject;

/**
 * Runs an Reactor @{@link HttpServer} with all registered @{@link RequestHandler}s.
 */
@Component
@EnableAutoConfiguration
public class RwServer2 implements CommandLineRunner {

    @Inject
    private ConfigFactory configFactory;

    @Override
    public void run(String... args) throws Exception {
        main(args);
    }


    public static void main(String[] args) {
        //Logic
        System.out.println("hello");
    }
}
