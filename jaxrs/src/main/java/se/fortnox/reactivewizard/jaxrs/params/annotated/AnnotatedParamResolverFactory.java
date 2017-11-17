package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.params.ParamResolver;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.Deserializer;

import javax.ws.rs.DefaultValue;
import java.lang.annotation.Annotation;

public interface AnnotatedParamResolverFactory {
    <T> ParamResolver<T> create(Annotation annotation, Deserializer<T> deserializer,
	    DefaultValue defaultValueAnnotation);
}
