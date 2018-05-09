package se.fortnox.reactivewizard.validation;

import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ParameterNameProvider;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Resolves field names from JAX-RS annotations so that api responses shows the name of the JAX-RS parameter rather than
 * the java parameter name.
 */
class JaxRsParameterNameResolver implements ParameterNameProvider {

    @Override
    public List<String> getParameterNames(Constructor<?> constructor) {
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        List<String> names = new ArrayList<>(parameterTypes.length);
        for (int i = 0; i < parameterTypes.length; i++) {
            names.add("arg" + i);
        }
        return names;
    }

    @Override
    public List<String> getParameterNames(Method method) {
        List<List<Annotation>> parameterAnnotations = ReflectionUtil.getParameterAnnotations(method);
        List<String> parameterNames = new ArrayList<>(parameterAnnotations.size());
        int index = 0;
        for (List<Annotation> annotations : parameterAnnotations) {
            parameterNames.add(getName(annotations, index++));
        }
        return parameterNames;
    }

    private String getName(List<Annotation> annotations, int index) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof QueryParam) {
                return ((QueryParam) annotation).value();
            }
            if (annotation instanceof PathParam) {
                return ((PathParam) annotation).value();
            }
            if (annotation instanceof FormParam) {
                return ((FormParam) annotation).value();
            }
            if (annotation instanceof HeaderParam) {
                return ((HeaderParam) annotation).value();
            }
        }
        return "arg" + index;
    }

}
