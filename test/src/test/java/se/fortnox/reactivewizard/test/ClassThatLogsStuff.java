package se.fortnox.reactivewizard.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ClassThatLogsStuff {
    private static final Logger LOG = LoggerFactory.getLogger(ClassThatLogsStuff.class);

    void logInfo(String message) {
        LOG.atInfo()
            .log(message);
    }
}
