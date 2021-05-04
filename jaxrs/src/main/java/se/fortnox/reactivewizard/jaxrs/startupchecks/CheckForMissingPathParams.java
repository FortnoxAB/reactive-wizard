package se.fortnox.reactivewizard.jaxrs.startupchecks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.jaxrs.JaxRsResource;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Matcher;

import static se.fortnox.reactivewizard.jaxrs.startupchecks.CheckForDuplicatePaths.PATH_PARAM_PATTERN;
import static se.fortnox.reactivewizard.jaxrs.startupchecks.CheckForDuplicatePaths.findParamWithPathParamAnnotation;

public class CheckForMissingPathParams implements StartupCheck {
    private static final Logger             LOG = LoggerFactory.getLogger(CheckForMissingPathParams.class);
    private final        StartupCheckConfig startupCheckConfig;

    @Inject
    public CheckForMissingPathParams(StartupCheckConfig startupCheckConfig) {
        this.startupCheckConfig = startupCheckConfig;
    }

    @Override
    public void check(List<JaxRsResource> resources) {
        resources.forEach(resource -> {
            Matcher matcher = PATH_PARAM_PATTERN.matcher(resource.getPath());
            while (matcher.find()) {
                final Method method           = resource.getResourceMethod();
                final String paramName        = matcher.group(1);
                final Method overriddenMethod = ReflectionUtil.getOverriddenMethod(method);

                Type type = findParamWithPathParamAnnotation(overriddenMethod != null ? overriddenMethod : resource.getResourceMethod(), paramName);
                if (type == null) {
                    final String message = "Could not find @PathParam annotated parameter for " + paramName + " on method " + method.toGenericString();
                    if (startupCheckConfig.isFailOnError()) {
                        throw new IllegalStateException(message);
                    } else {
                        LOG.warn(message);
                    }
                }
            }
        });
    }
}
