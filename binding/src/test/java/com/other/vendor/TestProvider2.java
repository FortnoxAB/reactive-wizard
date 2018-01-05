package com.other.vendor;

import javax.inject.Inject;
import com.google.inject.Provider;

public class TestProvider2 implements Provider<TestInterface> {
    @Inject
    public TestProvider2() {
    }

    @Override
    public TestInterface get() {
        return null;
    }
}
