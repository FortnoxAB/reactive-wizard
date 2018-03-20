package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extend this class and implement the visit method to scan the classpath at application startup. Your class must be in
 * this package to be found.
 *
 * You can inject your subclass into a AutoBindModule implementation in order to use the classes found for bindings.
 */
public abstract class AbstractClassScanner {
    private Set<Class<?>> classes = new HashSet<>();

    public abstract void visit(FastClasspathScanner classpathScanner);

    public void add(Class<?> cls) {
        classes.add(cls);
    }

    public Set<Class<?>> getClasses() {
        return classes;
    }
}
