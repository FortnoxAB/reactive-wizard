package com.other.vendor;

import javax.inject.Inject;
import javax.inject.Provider;

public class TestProvider implements Provider<TestInterface> {
    @Inject
    public TestProvider() {
    }

    @Override
    public TestInterface get() {
        return null;
    }
}
