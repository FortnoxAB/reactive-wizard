package se.fortnox.reactivewizard.jaxrs.params;

import java.lang.reflect.Parameter;

/**
 * A factory that can create a param resolver for a given parameter.
 *
 * @param <T>
 */
public interface ParamResolverFactory<T> {
    ParamResolver<T> createParamResolver(Parameter parameter);
}
