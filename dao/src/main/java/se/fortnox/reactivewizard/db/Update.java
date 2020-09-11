package se.fortnox.reactivewizard.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Update {
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    String value();

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    int minimumAffected() default 1;
}
