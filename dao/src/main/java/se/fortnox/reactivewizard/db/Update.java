package se.fortnox.reactivewizard.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@SuppressWarnings("checkstyle:MissingJavadocMethod")
public @interface Update {
    String value();

    int minimumAffected() default 1;
}
