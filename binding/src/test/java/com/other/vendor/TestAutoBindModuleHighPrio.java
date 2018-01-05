package com.other.vendor;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class TestAutoBindModuleHighPrio implements AutoBindModule {
    @Override
    public void configure(Binder binder) {
        binder.bind(InjectedInTestPrio.class).toInstance(new InjectedInTestPrio(Source.FROM_AUTO_BIND_MODULE_HIGH_PRIO));
    }

    @Override
    public Integer getPrio() {
        return 1000;
    }
}
