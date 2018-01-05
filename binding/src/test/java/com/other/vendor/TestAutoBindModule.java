package com.other.vendor;

import com.google.inject.Binder;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class TestAutoBindModule implements AutoBindModule {
    @Override
    public void configure(Binder binder) {
        binder.bind(InjectedInTest.class).toInstance(new InjectedInTest(Source.FROM_AUTO_BIND_MODULE));
        binder.bind(InjectedInTestPrio.class).toInstance(new InjectedInTestPrio(Source.FROM_AUTO_BIND_MODULE));
    }
}
