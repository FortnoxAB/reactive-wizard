package se.fortnox.reactivewizard.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Means that the resource returns a stream of data, rather than a single data item.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Stream {
    /**
     * Stream format. If it set to CONCATENATED_JSON_OBJECTS, then the format
     * will be a concatenation of json objects, for example:
     *
     * <pre>
     * {"value":"Hello"}{"value":"World"}
     * </pre>
     * If it is set to JSON_ARRAY then it will wrap the stream in a JSON array,
     * for example:
     * <pre>
     * [{"value":"Hello"},{"Value":"World"}]
     * </pre>
     * This option applies only for the resource methods returning Flux.
     *
     * @return a stream type
     */
    Type value() default Type.JSON_ARRAY;

    enum Type {
        @Deprecated(forRemoval = true)
        CONCATENATED_JSON_OBJECTS,
        JSON_ARRAY
    }
}
