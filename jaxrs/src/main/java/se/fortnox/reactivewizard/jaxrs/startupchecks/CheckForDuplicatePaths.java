package se.fortnox.reactivewizard.jaxrs.startupchecks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.inject.Inject;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckForDuplicatePaths implements StartupCheck {

    private final        StartupCheckConfig startupCheckConfig;
    private static final Logger             LOG                = LoggerFactory.getLogger(CheckForDuplicatePaths.class);
    public static final  Pattern            PATH_PARAM_PATTERN = Pattern.compile("\\{(\\w*)\\}");

    @Inject
    public CheckForDuplicatePaths(StartupCheckConfig startupCheckConfig) {
        this.startupCheckConfig = startupCheckConfig;
    }

    @Override
    public void check(List<JaxRsResource> resources) {
        resources.forEach(jaxRsResource -> {
            final Optional<JaxRsResource> duplicate = resources.stream()
                .filter(res -> res != jaxRsResource && pathParamCollides(res, jaxRsResource))
                .findFirst();

            if (duplicate.isPresent()) {
                String errorMessage = String.format("%s duplicates %s on path %s",
                    duplicate.get().getResourceMethod(),
                    jaxRsResource.getResourceMethod(),
                    duplicate.get().getPath());
                if (startupCheckConfig.isFailOnError()) {
                    throw new IllegalStateException(errorMessage);
                } else {
                    LOG.warn(errorMessage);
                }
            }
        });
    }

    public static boolean pathParamCollides(JaxRsResource jaxRsResource1, JaxRsResource jaxRsResource2) {
        if (jaxRsResource2 == null) {
            return false;
        }

        final Matcher thisMatcher = PATH_PARAM_PATTERN.matcher(jaxRsResource1.getPath());
        final Matcher thatMatcher = PATH_PARAM_PATTERN.matcher(jaxRsResource2.getPath());

        boolean pathParamCollision = false;
        while (thisMatcher.find() && thatMatcher.find()) {
            final Type thisPathParamType = getTypeOfParam(thisMatcher.group(1), jaxRsResource1.getResourceMethod());
            final Type thatPathParamType = getTypeOfParam(thatMatcher.group(1), jaxRsResource2.getResourceMethod());

            if (!Objects.equals(thisPathParamType, thatPathParamType)) {
                pathParamCollision = true;
            }
        }

        //Different verbs causes no collision
        if (!jaxRsResource1.getHttpMethod().equals(jaxRsResource2.getHttpMethod())) {
            return false;
        }

        final String thisPath          = createPathPattern(jaxRsResource1.getPath()).toString();
        final String jaxRsResourcePath = createPathPattern(jaxRsResource2.getPath()).toString();

        //The pattern differs somehow
        if (!thisPath.equalsIgnoreCase(jaxRsResourcePath)) {
            return false;
        }

        return pathParamCollision || checkOtherParameters(jaxRsResource1, jaxRsResource2);
    }

    private static Type getTypeOfParam(String pathParamName, Method method) {
        final Method overriddenMethod = ReflectionUtil.getOverriddenMethod(method);
        return findParamWithPathParamAnnotation(overriddenMethod != null ? overriddenMethod : method, pathParamName);
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
        Method thisMethod = ReflectionUtil.getOverriddenMethod(resource1.getInstanceMethod());
        if (thisMethod == null) {
            thisMethod = resource1.getInstanceMethod();
        }

        Method thatMethod = ReflectionUtil.getOverriddenMethod(resource2.getInstanceMethod());
        if (thatMethod == null) {
            thatMethod = resource2.getInstanceMethod();
        }

        Set<String> thisParams = extractParameterAnnotations(thisMethod);
        Set<String> thatParams = extractParameterAnnotations(thatMethod);

        return thisParams.equals(thatParams);
    }

    private static Set<String> extractParameterAnnotations(Method method) {
        Set<String>    otherParams     = new HashSet<>();
        Annotation[][] annotationArray = method.getParameterAnnotations();
        for (Annotation[] annotationRow : annotationArray) {
            for (Annotation annotation : annotationRow) {
                if (annotation instanceof QueryParam) {
                    QueryParam param = (QueryParam)annotation;
                    otherParams.add("QueryParam: " + param.value());
                } else if (annotation instanceof HeaderParam) {
                    HeaderParam param = (HeaderParam)annotation;
                    otherParams.add("HeaderParam: " + param.value());
                } else if (annotation instanceof CookieParam) {
                    CookieParam param = (CookieParam)annotation;
                    otherParams.add("CookieParam: " + param.value());
                }
            }
        }
        return otherParams;
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
