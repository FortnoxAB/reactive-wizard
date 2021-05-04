package se.fortnox.reactivewizard.jaxrs;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.jaxrs.startupchecks.StartupCheck;
import se.fortnox.reactivewizard.jaxrs.startupchecks.StartupCheckScanner;

public class JaxRsModule implements AutoBindModule {

    private final StartupCheckScanner startupCheckScanner;

    @Inject
    public JaxRsModule(StartupCheckScanner startupCheckScanner) {
        this.startupCheckScanner = startupCheckScanner;
    }

    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, JaxRsResourceInterceptor.class);
        Multibinder<StartupCheck> startupCheckBinder = Multibinder.newSetBinder(binder, StartupCheck.class);
        startupCheckScanner.getStartupChecks().forEach(startupCheck -> {
            startupCheckBinder.addBinding().to(startupCheck);
        });
    }
}
