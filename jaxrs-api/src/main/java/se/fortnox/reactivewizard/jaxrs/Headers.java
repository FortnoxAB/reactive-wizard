package se.fortnox.reactivewizard.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a resource method with this to make it set the header of your choice
 * <p>
 * e.g.
 * <p>
 * {@literal @}Headers("Content-Disposition: attachment; filename=export.csv")
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Headers {
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    String[] value();
}
