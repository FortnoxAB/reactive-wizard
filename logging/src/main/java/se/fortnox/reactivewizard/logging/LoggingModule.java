package se.fortnox.reactivewizard.logging;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class LoggingModule implements AutoBindModule {

    @Override
    public void configure(Binder binder) {
        binder.bind(LoggingBootstap.class).asEagerSingleton();
    }

    @Override
    public Integer getPrio() {
        // High prio to ensure logging is initiated early
        return Integer.MAX_VALUE;
    }
}
