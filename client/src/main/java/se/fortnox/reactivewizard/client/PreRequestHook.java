package se.fortnox.reactivewizard.client;

/**
 * Provides means of modifying an outgoing request before it is sent.
 */
public interface PreRequestHook {
    void apply(RequestBuilder request);
}
