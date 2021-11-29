package se.fortnox.reactivewizard;

import com.google.inject.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;
import se.fortnox.reactivewizard.binding.AutoBindModule;

/**
 * Module for setting up the environment.
 */
public class SetupModule implements AutoBindModule {
    private static final Logger LOG = LoggerFactory.getLogger("Hooks.onErrorDropped");

    @Override
    public void configure(Binder binder) {
        Hooks.onErrorDropped(throwable -> {
            logError(LOG, throwable);
        });
    }

    /**
     * package private to be able to be tested without too much trouble.
     *
     * @param logger the logger to be used
     * @param throwable the exception
     */
    void logError(Logger logger, Throwable throwable) {
        logger.warn("Tried to send item or error to subscriber but the subscriber had already left. " +
            "This could happen when you merge two (or more) observables and one reports an error while the other a moment later tries to " +
            "call onError on the subscriber but the subscriber already left at the first error.", throwable);
    }
}
