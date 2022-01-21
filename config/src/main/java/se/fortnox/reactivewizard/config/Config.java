package se.fortnox.reactivewizard.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark a class as being a config class, to be read from a file.
 * The value of the annotation should be the element name in the root of the file.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Config {
    /**
     * The name of the config.
     * @return the name
     */
    String value();
}
