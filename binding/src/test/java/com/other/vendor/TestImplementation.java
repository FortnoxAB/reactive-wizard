package com.other.vendor;

import javax.inject.Inject;

public class TestImplementation implements TestInterface {

    @Inject
    public TestImplementation() {
    }

    @Override
    public Source getSource() {
        return Source.FROM_IMPLEMENTATION;
    }
}
