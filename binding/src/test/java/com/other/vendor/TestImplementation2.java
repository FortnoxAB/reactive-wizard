package com.other.vendor;

import jakarta.inject.Inject;

public class TestImplementation2 implements TestInterface {

    @Inject
    public TestImplementation2() {
    }

    @Override
    public Source getSource() {
        return Source.FROM_IMPLEMENTATION_2;
    }
}
