package se.fortnox.reactivewizard.server;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;

import javax.inject.Singleton;

/**
 * Makes all necessary bindings for setting up an @{@link RwServer} with @{@link JaxRsRequestHandler}.
 */
@Singleton
public class ServerModule implements AutoBindModule {

    @Override
    public void configure(Binder binder) {
        binder.bind(RwServer.class).asEagerSingleton();
    }
}
