package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * Extend this class and implement the visit method to scan the classpath at application startup. Your class must be in
 * this package to be found.
 *
 * You can inject your subclass into a AutoBindModule implementation in order to use the classes found for bindings.
 */
public abstract class AbstractClassScanner {
    private List<Class<?>> classes = new ArrayList<>();

    public abstract void visit(FastClasspathScanner classpathScanner);

    public void add(Class<?> cls) {
        classes.add(cls);
    }

    public List<Class<?>> getClasses() {
        return classes;
    }
}
