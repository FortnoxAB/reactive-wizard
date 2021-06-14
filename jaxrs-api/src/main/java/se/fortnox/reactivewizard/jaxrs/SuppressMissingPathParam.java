package se.fortnox.reactivewizard.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a resource method/interface with this annotation and list what paramName(s) you want to exclude from the missing path param startup check.
 * <p>To ignore missing path param errors on a resource with path <i>/api/{name}</i> simply add <i>@SuppressMissingPathParam(paramNames = "name")</i> to the resource method.</p>
 * <p><b>Note:</b> paramNames are case sensitive.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SuppressMissingPathParam {
    String[] paramNames() default "";
}
