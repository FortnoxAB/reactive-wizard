package se.fortnox.reactivewizard.jaxrs.response;

/**
 * Interface for transforming a result of a JaxRs call.
 */
public interface ResultTransformer<T> {

	JaxRsResult<T> apply(JaxRsResult<T> result, Object[] args);
}
