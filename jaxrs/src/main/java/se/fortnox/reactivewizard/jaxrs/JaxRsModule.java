package se.fortnox.reactivewizard.jaxrs;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class JaxRsModule implements AutoBindModule {

    private final StartupCheckScanner startupCheckScanner;

    @Inject
    public JaxRsModule(StartupCheckScanner startupCheckScanner) {
        this.startupCheckScanner = startupCheckScanner;
    }

    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, JaxRsResourceInterceptor.class);

        startupCheckScanner.getStartupChecks().forEach(startupCheck -> {
            binder.bind(startupCheck).asEagerSingleton();
        });
    }
}
