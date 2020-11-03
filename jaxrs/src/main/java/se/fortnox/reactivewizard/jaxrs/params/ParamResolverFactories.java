package se.fortnox.reactivewizard.jaxrs.params;

import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.jaxrs.WebException;
import se.fortnox.reactivewizard.jaxrs.params.annotated.AnnotatedParamResolverFactories;
import se.fortnox.reactivewizard.jaxrs.params.annotated.AnnotatedParamResolverFactory;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.BodyDeserializer;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerException;
import se.fortnox.reactivewizard.jaxrs.params.deserializing.DeserializerFactory;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;


/**
 * Creates param resolvers which can resolve method parameters from an incoming request.
 */
public class ParamResolverFactories {

    private final DeserializerFactory             deserializerFactory;
    private final ParamResolvers                  paramResolvers;
    private final AnnotatedParamResolverFactories annotatedParamResolverFactories;
    private final ParamTypeResolver               paramTypeResolver;

    @Inject
    public ParamResolverFactories(DeserializerFactory deserializerFactory,
        ParamResolvers paramResolvers,
        AnnotatedParamResolverFactories annotatedParamResolverFactories,
        ParamTypeResolver paramTypeResolver
    ) {
        this.deserializerFactory = deserializerFactory;
        this.paramResolvers = paramResolvers;
        this.annotatedParamResolverFactories = annotatedParamResolverFactories;
        this.paramTypeResolver = paramTypeResolver;
    }

    public ParamResolverFactories() {
        this(new DeserializerFactory(),
            new ParamResolvers(),
            new AnnotatedParamResolverFactories(),
            new WrapSupportingParamTypeResolver());
    }

    public List<ParamResolver> createParamResolvers(Method method, String[] consumesAnnotation) {
        List<ParamResolver> paramResolvers = new ArrayList<>();

        Method      interfaceMethod     = ReflectionUtil.getOverriddenMethod(method);
        Parameter[] instanceParameters  = method.getParameters();
        Parameter[] interfaceParameters = interfaceMethod == null ? instanceParameters : interfaceMethod.getParameters();

        for (int i = 0; i < instanceParameters.length; i++) {
            TypeReference<?> paramType            = paramTypeResolver.resolveParamType(instanceParameters[i], interfaceParameters[i]);
            List<Annotation> parameterAnnotations = ReflectionUtil.getParameterAnnotations(interfaceParameters[i]);
            paramResolvers.add(createParamResolver(instanceParameters[i], paramType, parameterAnnotations, consumesAnnotation));
        }
        return paramResolvers;
    }

    protected <T> ParamResolver<T> createParamResolver(Parameter parameter,
        TypeReference<T> paramType,
        List<Annotation> parameterAnnotations,
        String[] consumesAnnotation
    ) {
        for (Annotation annotation : parameterAnnotations) {
            AnnotatedParamResolverFactory paramResolverFactory = annotatedParamResolverFactories.get(annotation.annotationType());
            if (paramResolverFactory != null) {
                String defaultValue = findDefaultValue(parameterAnnotations);
                return paramResolverFactory.create(paramType, annotation, defaultValue);
            }
        }
        ParamResolverFactory<T> paramResolverFactory = paramResolvers.get((Class<T>)ReflectionUtil.getRawType(paramType.getType()));
        if (paramResolverFactory != null) {
            ParamResolver<T> paramResolver = paramResolverFactory.createParamResolver(parameter);
            if (paramResolver != null) {
                return paramResolver;
            }
        }

        BodyDeserializer<T> bodyDeserializer = deserializerFactory.getBodyDeserializer(paramType, consumesAnnotation);
        if (bodyDeserializer != null) {
            return request -> Mono.just(deserializeBody(bodyDeserializer, request.getBody()));
        }

        throw new RuntimeException("Could not find any deserializer for param of type " + paramType.getType());
    }

    public static String findDefaultValue(List<Annotation> parameterAnnotations) {
        DefaultValue defaultValueAnnotation = findDefaultValueAnnotation(parameterAnnotations);
        if (defaultValueAnnotation == null) {
            return null;
        }
        return defaultValueAnnotation.value();
    }

    public static DefaultValue findDefaultValueAnnotation(List<Annotation> parameterAnnotations) {
        for (Annotation annotation : parameterAnnotations) {
            if (DefaultValue.class == annotation.annotationType()) {
                return (DefaultValue)annotation;
            }
        }
        return null;
    }

    private <T> T deserializeBody(BodyDeserializer<T> deserializer, byte[] value) {
        try {
            return deserializer.deserialize(value);
        } catch (DeserializerException deserializerException) {
            throw new WebException(HttpResponseStatus.BAD_REQUEST, deserializerException.getMessage());
        }
    }
}
