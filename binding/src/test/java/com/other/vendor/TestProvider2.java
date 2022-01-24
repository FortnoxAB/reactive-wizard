package com.other.vendor;

import com.google.inject.Provider;

import javax.inject.Inject;

public class TestProvider2 implements Provider<TestInterface> {
    @Inject
    public TestProvider2() {
    }

    @Override
    public TestInterface get() {
        return null;
    }
}
