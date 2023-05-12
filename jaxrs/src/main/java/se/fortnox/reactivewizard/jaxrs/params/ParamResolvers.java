package se.fortnox.reactivewizard.jaxrs.params;

import se.fortnox.reactivewizard.jaxrs.JaxRsRequest;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

/**
 * Finds a ParamResolverFactory for a given type.
 */
public class ParamResolvers {

    private final HashMap<Class<?>, ParamResolverFactory> paramResolversMap;

    @Inject
    public ParamResolvers(Set<ParamResolver> paramResolvers, Set<ParamResolverFactory> paramResolverFactories) {
        paramResolversMap = new HashMap<>();
        for (ParamResolver paramResolverFactory : paramResolvers) {
            paramResolversMap.put(getResolverTargetClass(paramResolverFactory), asFactory(paramResolverFactory));
        }
        for (ParamResolverFactory factory : paramResolverFactories) {
            paramResolversMap.put(getResolverTargetClass(factory), factory);
        }
    }

    public ParamResolvers(ParamResolver... paramResolvers) {
        this(new HashSet<>(asList(paramResolvers)), Collections.emptySet());
    }

    public ParamResolvers() {
        this(Collections.EMPTY_SET, Collections.EMPTY_SET);
    }

    private ParamResolverFactory asFactory(ParamResolver paramResolver) {
        return parameter -> paramResolver;
    }

    private Class<?> getResolverTargetClass(ParamResolver paramResolver) {
        try {
            Method method = paramResolver.getClass().getMethod("resolve", JaxRsRequest.class);
            return ReflectionUtil.getRawType(ReflectionUtil.getTypeOfFluxOrMono(method));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> getResolverTargetClass(ParamResolverFactory paramResolverFactory) {
        try {
            Method method = paramResolverFactory.getClass().getMethod("createParamResolver", Parameter.class);

            ParameterizedType parameterizedType = (ParameterizedType)method.getGenericReturnType();
            return ReflectionUtil.getRawType(parameterizedType.getActualTypeArguments()[0]);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> ParamResolverFactory<T> get(Class<T> paramType) {
        return paramResolversMap.get(paramType);
    }
}
