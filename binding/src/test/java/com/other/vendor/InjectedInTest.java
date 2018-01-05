package com.other.vendor;

import javax.inject.Inject;

public class InjectedInTest {
    private final Source source;

    @Inject
    public InjectedInTest() {
        this.source = Source.DEFAULT;
    }

    public InjectedInTest(Source source) {
        this.source = source;
    }

    public Source getSource() {
        return source;
    }
}
