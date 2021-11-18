package se.fortnox.reactivewizard.server;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.netty.http.server.HttpServer;
import se.fortnox.reactivewizard.RequestHandler;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.config.ConfigFactory;

/**
 * Runs an Reactor @{@link HttpServer} with all registered @{@link RequestHandler}s.
 */
public class RwServerConfigOnlyAutoBindModule implements AutoBindModule {

    private AnnotationConfigReactiveWebServerApplicationContext reactiveWebServerApplicationContext;

    @Inject
    public RwServerConfigOnlyAutoBindModule() {
        //reactiveWebServerApplicationContext =
        //    new AnnotationConfigReactiveWebServerApplicationContext(RwServerConfig.class);
        System.out.println("Creaded rwserver config only");
    }

    @Override
    public void configure(Binder binder) {
        //binder.bind(ApplicationContext.class).toInstance(reactiveWebServerApplicationContext);
    }
}
