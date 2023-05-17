package com.other.vendor;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class TestProvider implements Provider<TestInterface> {
    @Inject
    public TestProvider() {
    }

    @Override
    public TestInterface get() {
        return null;
    }
}
