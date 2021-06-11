package se.fortnox.reactivewizard.jaxrs;

import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import static se.fortnox.reactivewizard.jaxrs.CheckForCollidingPaths.PATH_PARAM_PATTERN;
import static se.fortnox.reactivewizard.jaxrs.CheckForCollidingPaths.findParamWithPathParamAnnotation;

public class CheckForMissingPathParams {

    private CheckForMissingPathParams() {
    }

    static void check(List<JaxRsResource> resources) {
        resources.forEach(resource -> {
            Matcher matcher = PATH_PARAM_PATTERN.matcher(resource.getPath());
            while (matcher.find()) {
                final Method method           = resource.getResourceMethod();
                final String paramName        = matcher.group(1);
                final Method overriddenMethod = ReflectionUtil.getOverriddenMethod(method);

                if (shouldSuppressErrors(resource, method, paramName)) {
                    continue;
                }

                Type type = findParamWithPathParamAnnotation(overriddenMethod != null ? overriddenMethod : resource.getResourceMethod(), paramName);
                if (type == null) {
                    final String message = String.format(
                        "Could not find @PathParam annotated parameter for %s on method %s. " +
                            "If this is intended, the error can be suppressed with @SupressMissingPathParam.",
                        paramName,
                        method.toGenericString()
                    );
                    throw new IllegalStateException(message);
                }
            }
        });
    }

    private static boolean shouldSuppressErrors(JaxRsResource resource, Method method, String paramName) {
        SuppressMissingPathParam methodAnnotation = ReflectionUtil.getAnnotation(resource.getInstanceMethod(), SuppressMissingPathParam.class);
        if (methodAnnotation != null) {
            return Arrays.asList(methodAnnotation.paramName())
                .contains(paramName);
        }

        Class<?>                 declaringClass  = method.getDeclaringClass();
        SuppressMissingPathParam classAnnotation = declaringClass.getAnnotation(SuppressMissingPathParam.class);
        return classAnnotation != null && Arrays.asList(classAnnotation.paramName())
            .contains(paramName);
    }
}
