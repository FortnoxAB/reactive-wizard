package se.fortnox.reactivewizard.client;

/**
 * Implement this interface to add custom serialization of request parameters.
 */
public interface RequestParameterSerializer<T> {
    void addParameter(T param, RequestBuilder request);
}
