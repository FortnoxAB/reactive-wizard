package se.fortnox.reactivewizard.jaxrs.params.annotated;

import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class AnnotatedParamResolverFactories {
    private final Map<Class<? extends Annotation>, AnnotatedParamResolverFactory> paramExtractorFactories;

    @Inject
    public AnnotatedParamResolverFactories(DeserializerFactory deserializerFactory) {
        paramExtractorFactories = new HashMap<>();
        paramExtractorFactories.put(QueryParam.class, new QueryParamResolver.Factory(deserializerFactory));
        paramExtractorFactories.put(PathParam.class, new PathParamResolver.Factory(deserializerFactory));
        paramExtractorFactories.put(HeaderParam.class, new HeaderParamResolver.Factory(deserializerFactory));
        paramExtractorFactories.put(FormParam.class, new FormParamResolver.Factory(deserializerFactory));
        paramExtractorFactories.put(CookieParam.class, new CookieParamResolver.Factory(deserializerFactory));
        paramExtractorFactories.put(BeanParam.class, new BeanParamResolver.Factory(this));
    }

    public AnnotatedParamResolverFactories() {
        this(new DeserializerFactory());
    }

    public AnnotatedParamResolverFactory get(Class<? extends Annotation> annotationClass) {
        return paramExtractorFactories.get(annotationClass);
    }
}
