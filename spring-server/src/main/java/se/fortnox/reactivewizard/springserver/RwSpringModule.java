package se.fortnox.reactivewizard.springserver;

import com.google.inject.Binder;
import org.springframework.guice.module.SpringModule;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.inject.Inject;
import javax.inject.Named;

public class RwSpringModule extends SpringModule implements AutoBindModule {

    public static Binder binder;

    private final String[] args;

    @Inject
    public RwSpringModule(@Named("args") String[] args) {
        super(() -> {
            final MySpringApplication instance = MySpringApplication.getInstance(RwServerConfig.class);
            return instance.createApplicationContext().getBeanFactory();
        });

        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        RwSpringModule.binder = binder;
        super.configure(binder);

        binder.bind(SpringStart.class).asEagerSingleton();

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);
        binder.bind(ServerConfig.class).toProvider(() -> serverConfig);
    }

    @Override
    public Integer getPrio() {
        return AutoBindModule.super.getPrio() + 1;
    }

}
