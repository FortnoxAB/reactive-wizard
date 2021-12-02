package se.fortnox.reactivewizard.springserver;

import com.google.inject.Binder;
import org.springframework.guice.module.SpringModule;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.server.ServerConfig;

import javax.inject.Inject;
import javax.inject.Named;

import static se.fortnox.reactivewizard.springserver.RwSpringApplication.getInstance;

public class RwSpringModule extends SpringModule implements AutoBindModule {

    public static Binder binder;

    private final String[] args;

    @Inject
    public RwSpringModule(@Named("args") String[] args) {
        super(() -> {

            //Provide the spring module with a context
            return getInstance(RwServerConfig.class).createApplicationContext().getBeanFactory();

        });

        this.args = args;
    }

    @Override
    public void configure(Binder binder) {
        super.configure(binder);

        startupSpringServer(binder);

        disableRwServer(binder);
    }


    private void startupSpringServer(Binder binder) {
        binder.bind(SpringStart.class).asEagerSingleton();
    }

    /**
     * Causes the rw server not to start
     * @param binder the binder to use
     */
    private void disableRwServer(Binder binder) {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);
        binder.bind(ServerConfig.class).toProvider(() -> serverConfig);
    }

    @Override
    public Integer getPrio() {
        return AutoBindModule.super.getPrio() + 1;
    }

}
