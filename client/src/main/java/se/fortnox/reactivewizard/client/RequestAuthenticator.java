package se.fortnox.reactivewizard.client;

/**
 * Something that is able to make an outgoing request authenticated.
 */
public interface RequestAuthenticator {
    void authenticate(RequestBuilder request);
}
