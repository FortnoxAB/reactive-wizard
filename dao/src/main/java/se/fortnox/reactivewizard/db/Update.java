package se.fortnox.reactivewizard.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Update {
    /**
     * The update query.
     * @return the query.
     */
    String value();

    /**
     * Number of rows to be affected, not to be considered an error.
     * @return number of rows
     */
    int minimumAffected() default 1;
}
