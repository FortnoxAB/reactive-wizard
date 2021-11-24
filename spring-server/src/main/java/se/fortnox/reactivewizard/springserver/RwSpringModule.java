package se.fortnox.reactivewizard.springserver;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.springframework.boot.SpringApplication;
import org.springframework.guice.module.SpringModule;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.server.ServerConfig;

public class RwSpringModule extends SpringModule implements AutoBindModule {

    @Inject
    public RwSpringModule(@Named("args") String[] args) {
        super(SpringApplication.run(RwServerConfig.class, args));
    }

    @Override
    public void configure() {
        bind(MappingSetup.class).asEagerSingleton();
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);
        bind(ServerConfig.class).toProvider(() -> serverConfig);
    }

    @Override
    public Integer getPrio() {
        return AutoBindModule.super.getPrio() + 1;
    }
}
