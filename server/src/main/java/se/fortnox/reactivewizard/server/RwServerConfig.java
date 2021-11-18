package se.fortnox.reactivewizard.server;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@EnableAutoConfiguration
public class RwServerConfig {
    public RwServerConfig(CompositeRequestHandler compositeRequestHandler, ApplicationContext applicationContext) {
        System.out.println("Got a composite request handler: " + (compositeRequestHandler != null));
    }
}
