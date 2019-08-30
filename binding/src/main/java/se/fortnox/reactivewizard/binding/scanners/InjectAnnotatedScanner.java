package se.fortnox.reactivewizard.binding.scanners;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import javax.inject.Singleton;

/**
 * Finds all classes with @Inject annotated constructors.
 */
@Singleton
public class InjectAnnotatedScanner extends AbstractClassScanner {
    @Override
    public void visit(ScanResult scanResult) {
        scanResult.getClassesWithMethodAnnotation(javax.inject.Inject.class.getName()).forEach(this::classFound);
        scanResult.getClassesWithMethodAnnotation(com.google.inject.Inject.class.getName()).forEach(this::classFound);
    }

    private void classFound(ClassInfo classInfo) {
        if (classInfo.getConstructorInfo()
                .stream()
                .anyMatch(mi->mi.hasAnnotation(javax.inject.Inject.class.getName()) || mi.hasAnnotation(com.google.inject.Inject.class.getName()))) {
            add(classInfo.loadClass());
        }
    }
}
