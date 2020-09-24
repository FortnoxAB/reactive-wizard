package se.fortnox.reactivewizard.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Query {
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    String value();

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    String[] allowedSortColumns() default {};

    /**
     * Applies default sorting if CollectionOptions does not have any.
     * Can be combined with sorting specified in @Query and will have more priority.
     * Example: defaultSort = "name desc"
     */
    String defaultSort() default "";

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    int defaultLimit() default 100;

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    int maxLimit() default 1000;
}
