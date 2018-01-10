package se.fortnox.reactivewizard.logging;

import javax.inject.Inject;

public class LoggingBootstap {
    @Inject
    public LoggingBootstap(LoggingFactory loggingFactory) {
        loggingFactory.init();
    }
}
