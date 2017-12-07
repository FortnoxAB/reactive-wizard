package se.fortnox.reactivewizard.jaxrs.params.annotated;

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

    public AnnotatedParamResolverFactories() {
        paramExtractorFactories = new HashMap<>();
        paramExtractorFactories.put(QueryParam.class, QueryParamResolver::new);
        paramExtractorFactories.put(PathParam.class, PathParamResolver::new);
        paramExtractorFactories.put(HeaderParam.class, HeaderParamResolver::new);
        paramExtractorFactories.put(FormParam.class, FormParamResolver::new);
        paramExtractorFactories.put(CookieParam.class, CookieParamResolver::new);
    }

    public AnnotatedParamResolverFactory get(Class<? extends Annotation> annotationClass) {
        return paramExtractorFactories.get(annotationClass);
    }
}
