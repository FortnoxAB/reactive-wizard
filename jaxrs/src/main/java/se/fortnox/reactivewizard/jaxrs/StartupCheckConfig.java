package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.config.Config;

@Config("startupcheck")
public class StartupCheckConfig {
    private boolean failOnError = true;

    public boolean isFailOnError() {
        return failOnError;
    }

    public StartupCheckConfig setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
        return this;
    }
}
