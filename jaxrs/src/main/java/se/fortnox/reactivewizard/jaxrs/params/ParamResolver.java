package se.fortnox.reactivewizard.jaxrs.params;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import rx.Observable;

/**
 * Interface for resolving custom parameter types from an incoming request. You can use this to implement authentication
 * for example.
 * @param <T> the type that this ParamResolver can resolve from a request
 */
public interface ParamResolver<T> {
	Observable<T> resolve(JaxRsRequest request);
}
