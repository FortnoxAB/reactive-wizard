package se.fortnox.reactivewizard.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Query {
    /**
     * The query.
     * @return the query
     */
    String value();

    /**
     * The columns that allow sorting.
     * @return the columns
     */
    String[] allowedSortColumns() default {};

    /**
     * Applies default sorting if CollectionOptions does not have any.
     * Can be combined with sorting specified in @Query and will have more priority.
     * Example: defaultSort = "name desc"
     * @return the default sorting
     */
    String defaultSort() default "";

    /**
     * Default result limit.
     * @return the limit
     */
    int defaultLimit() default 100;

    /**
     * Max result limit.
     * @return the limit
     */
    int maxLimit() default 1000;
}
