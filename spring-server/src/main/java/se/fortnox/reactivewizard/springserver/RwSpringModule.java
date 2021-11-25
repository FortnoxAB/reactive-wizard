package se.fortnox.reactivewizard.springserver;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.server.ServerConfig;

public class RwSpringModule implements AutoBindModule {

    @Override
    public void configure(Binder binder) {
        binder.bind(MappingSetup.class).asEagerSingleton();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);
        binder.bind(ServerConfig.class).toProvider(() -> serverConfig);
    }

    @Override
    public Integer getPrio() {
        return AutoBindModule.super.getPrio() + 1;
    }
}
