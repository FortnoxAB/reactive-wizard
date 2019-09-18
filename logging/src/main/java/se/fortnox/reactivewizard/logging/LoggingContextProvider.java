package se.fortnox.reactivewizard.logging;

import io.reactiverse.reactivecontexts.core.ContextProvider;
import org.slf4j.MDC;

import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * Provider for propagating the MDC (Mapped Diagnostic Context) across observable/thread boundaries.
 */
public class LoggingContextProvider implements ContextProvider<Map<String, String>> {

    @Override
    public Map<String, String> install(Map<String, String> newState) {
        Map<String, String> previousState = MDC.getCopyOfContextMap();
        nullSafeSet(newState);
        return previousState;
    }

    @Override
    public void restore(Map<String, String> previousState) {
        nullSafeSet(previousState);
    }

    @Override
    public Map<String, String> capture() {
        return MDC.getCopyOfContextMap();
    }

    private static void nullSafeSet(Map<String, String> state) {
        ofNullable(state)
            .ifPresent(MDC::setContextMap);
    }
}
