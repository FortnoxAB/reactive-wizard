package se.fortnox.reactivewizard.binding.scanners;

import java.util.Set;
import java.util.TreeSet;

/**
 * Extend this class and implement the visit method to scan the classpath at application startup. Your class must be in
 * this package to be found.
 *
 * You can inject your subclass into a AutoBindModule implementation in order to use the classes found for bindings.
 */
public abstract class AbstractClassScanner {
    private Set<Class<?>> classes = new TreeSet<>(this::compareClassByName);

    private int compareClassByName(Class<?> cls1, Class<?> cls2) {
        return cls1.getName().compareTo(cls2.getName());
    }

    public abstract void visit(ClassScanner classScanner);

    public void add(Class<?> cls) {
        classes.add(cls);
    }

    public Set<Class<?>> getClasses() {
        return classes;
    }
}
