package se.fortnox.reactivewizard;

import com.google.common.annotations.VisibleForTesting;
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
            warnLogException(LOG, throwable);
        });
    }

    /**
     * package private to be able to be tested without too much trouble.
     *
     * @param logger the logger to be used
     * @param throwable the exception
     */
    @VisibleForTesting
    void warnLogException(Logger logger, Throwable throwable) {
        logger.warn("Tried to send an onError signal to the subscriber but the subscriber had already left" +
            "This can happen when a publisher calls onError on the operator despite having already called onError on it previously and the spec says " +
            "never should a subscriber get two calls to onError. A typical example when this could happen is when you merge two observables and both " +
            "of them reports an error", throwable);
    }
}
