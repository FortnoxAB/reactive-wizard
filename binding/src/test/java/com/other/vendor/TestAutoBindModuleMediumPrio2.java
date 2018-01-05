package com.other.vendor;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class TestAutoBindModuleMediumPrio2 implements AutoBindModule {
    @Override
    public void configure(Binder binder) {
        binder.bind(InjectedInTestSamePrio.class).toInstance(new InjectedInTestSamePrio(Source.FROM_AUTO_BIND_MODULE_MED2));
    }

    @Override
    public Integer getPrio() {
        return 500;
    }
}
