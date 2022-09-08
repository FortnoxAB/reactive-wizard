package se.fortnox.reactivewizard.test;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation used with LoggingVerifierExtension to indicate for which class the injected LoggingVerifier should verify logging behaviour.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface LoggingVerifierFor {
    /**
     * Get the class for which the LoggingVerifier should be created.
     *
     * @return the class
     */
    Class<?> value();
}
