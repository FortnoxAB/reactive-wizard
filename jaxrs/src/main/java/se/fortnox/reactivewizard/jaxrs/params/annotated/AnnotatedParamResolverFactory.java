package se.fortnox.reactivewizard.jaxrs.params.annotated;

import com.fasterxml.jackson.core.type.TypeReference;
import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;

import java.lang.annotation.Annotation;

public interface AnnotatedParamResolverFactory {
    <T> ParamResolver<T> create(TypeReference<T> paramType, Annotation annotation, String defaultValue);
}
