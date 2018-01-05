package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;

/**
 * Finds all classes with @Inject annotated constructors.
 */
@Singleton
public class InjectAnnotatedScanner extends AbstractClassScanner {
    @Override
    public void visit(FastClasspathScanner fastClasspathScanner) {
        fastClasspathScanner.matchClassesWithMethodAnnotation(javax.inject.Inject.class, this::classFound);
        fastClasspathScanner.matchClassesWithMethodAnnotation(com.google.inject.Inject.class, this::classFound);
    }

    private void classFound(Class<?> cls, Executable executable) {
        if (executable instanceof Constructor) {
            add(cls);
        }
    }
}
