package se.fortnox.reactivewizard.springserver;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.inject.Inject;

/**
 * Only here to disable RwServer
 */
public class SpringAutobindModule implements AutoBindModule {
    private final ServerConfig serverConfig;

    @Inject
    public SpringAutobindModule(ConfigFactory configFactory) {
        serverConfig = configFactory.get(ServerConfig.class);
    }

    @Override
    public void configure(Binder binder) {
        serverConfig.setEnabled(false);
        binder.bind(ServerConfig.class).toInstance(serverConfig);
    }
}
