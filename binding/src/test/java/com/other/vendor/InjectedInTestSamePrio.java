package com.other.vendor;

import jakarta.inject.Inject;

public class InjectedInTestSamePrio {
    private final Source source;

    @Inject
    public InjectedInTestSamePrio() {
        this.source = Source.DEFAULT;
    }

    public InjectedInTestSamePrio(Source source) {
        this.source = source;
    }

    public Source getSource() {
        return source;
    }
}
