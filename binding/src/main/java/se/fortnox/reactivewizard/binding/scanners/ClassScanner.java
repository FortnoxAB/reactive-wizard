package se.fortnox.reactivewizard.binding.scanners;

import java.lang.annotation.Annotation;

/**
 * Used in classes extending AbstractClassScanner, to find classes on the classpath that will be used when creating
 * bindings.
 */
public interface ClassScanner {
    /**
     * Find all classes having a method with the given annotation
     * @param annotation the annotation that should be present on any method in the classes to find
     * @return classes found
     */
    Iterable<Class<?>> findClassesWithMethodAnnotation(Class<? extends Annotation> annotation);

    /**
     * Find all classes having the given annotation
     * @param annotation the annotation that should be present on the classes to find
     * @return classes found
     */
    Iterable<Class<?>> findClassesAnnotatedWith(Class<? extends Annotation> annotation);

    /**
     * Find all classes being subclasses of the given class
     * @param parentClass the class that is a parent of the classes to find
     * @return classes found
     */
    <T> Iterable<Class<? extends T>> findSubclassesOf(Class<T> parentClass);
}
