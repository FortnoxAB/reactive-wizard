package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CheckForCollidingPaths {

    public static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{(\\w*)\\}");

    static void check(List<JaxRsResource> resources) {
        List<JaxRsResource> resourcesToCheck = resources.stream()
            .filter(resource -> {
                SuppressPathCollision annotation = ReflectionUtil.getAnnotation(resource.getInstanceMethod(), SuppressPathCollision.class);
                return annotation == null;
            })
            .collect(Collectors.toList());

        ListIterator<JaxRsResource> iterator = resourcesToCheck.listIterator();
        while (iterator.hasNext()) {
            JaxRsResource currentResource = iterator.next();
            resourcesToCheck.listIterator(iterator.nextIndex())
                .forEachRemaining(suspect -> checkIfColliding(currentResource, suspect));
        }
    }

    private static void checkIfColliding(JaxRsResource resource1, JaxRsResource resource2) {
        if (pathParamCollides(resource1, resource2)) {
            String errorMessage = String.format("%s collides with %s on path %s",
                resource1.getInstanceMethod(),
                resource2.getInstanceMethod(),
                resource1.getPath()
            );
            throw new IllegalStateException(errorMessage);
        }
    }

    public static boolean pathParamCollides(JaxRsResource resource1, JaxRsResource resource2) {
        //Different verbs causes no collision
        if (!resource1.getHttpMethod().equals(resource2.getHttpMethod())) {
            return false;
        }

        final String resourcePath1 = createPathPattern(resource1.getPath()).toString();
        final String resourcePath2 = createPathPattern(resource2.getPath()).toString();

        //The pattern differs somehow
        if (!resourcePath1.equalsIgnoreCase(resourcePath2)) {
            return false;
        }

        return checkOtherParameters(resource1, resource2);
    }

    static Type findParamWithPathParamAnnotation(Method method, String pathParamName) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(PathParam.class) && parameter.getAnnotation(PathParam.class).value().equalsIgnoreCase(pathParamName)) {
                return parameter.getType();
            }
        }
        return null;
    }

    /**
     * Throws exception if we find a param in one method that is elsewhere defined as another type
     */
    private static boolean checkOtherParameters(JaxRsResource resource1, JaxRsResource resource2) {
        Method resourceMethod1 = ReflectionUtil.getOverriddenMethod(resource1.getInstanceMethod());
        if (resourceMethod1 == null) {
            resourceMethod1 = resource1.getInstanceMethod();
        }

        Method resourceMethod2 = ReflectionUtil.getOverriddenMethod(resource2.getInstanceMethod());
        if (resourceMethod2 == null) {
            resourceMethod2 = resource2.getInstanceMethod();
        }

        Set<String> resourceParams1 = extractParameterAnnotations(resourceMethod1);
        Set<String> resourceParams2 = extractParameterAnnotations(resourceMethod2);

        return resourceParams1.equals(resourceParams2);
    }

    private static Set<String> extractParameterAnnotations(Method method) {
        Set<String>    params          = new HashSet<>();
        Annotation[][] annotationArray = method.getParameterAnnotations();

        for (Annotation[] annotationRow : annotationArray) {
            for (Annotation annotation : annotationRow) {
                if (annotation instanceof QueryParam) {
                    QueryParam param = (QueryParam)annotation;
                    params.add("QueryParam: " + param.value());
                } else if (annotation instanceof HeaderParam) {
                    HeaderParam param = (HeaderParam)annotation;
                    params.add("HeaderParam: " + param.value());
                } else if (annotation instanceof CookieParam) {
                    CookieParam param = (CookieParam)annotation;
                    params.add("CookieParam: " + param.value());
                }
            }
        }
        return params;
    }

    private static Pattern createPathPattern(String path) {
        // Paths with custom regex, like this: {myvar:myregex}
        path = path.replaceAll("\\{([^}]+):([^}]+)\\}", "paramplaceholder");

        // Paths without custom regex, like this: {myvar}
        path = path.replaceAll("\\{([^}]+)\\}", "paramplaceholder");

        // Allow trailing slash
        path = "^" + path + "[/\\s]*$";

        return Pattern.compile(path);
    }

}
