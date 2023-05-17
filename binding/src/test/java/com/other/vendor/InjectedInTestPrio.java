package com.other.vendor;

import jakarta.inject.Inject;

public class InjectedInTestPrio {
    private final Source source;

    @Inject
    public InjectedInTestPrio() {
        this.source = Source.DEFAULT;
    }

    public InjectedInTestPrio(Source source) {
        this.source = source;
    }

    public Source getSource() {
        return source;
    }
}
