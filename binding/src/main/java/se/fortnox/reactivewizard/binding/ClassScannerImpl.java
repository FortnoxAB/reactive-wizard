package se.fortnox.reactivewizard.binding;

import io.github.classgraph.ScanResult;
import se.fortnox.reactivewizard.binding.scanners.ClassScanner;

import java.lang.annotation.Annotation;

public class ClassScannerImpl implements ClassScanner {
    private final ScanResult scanResult;

    public ClassScannerImpl(ScanResult scanResult) {
        this.scanResult = scanResult;
    }

    @Override
    public Iterable<Class<?>> findClassesWithMethodAnnotation(Class<? extends Annotation> annotation) {
        return scanResult.getClassesWithMethodAnnotation(annotation.getName()).loadClasses();
    }

    @Override
    public Iterable<Class<?>> findClassesAnnotatedWith(Class<? extends Annotation> annotation) {
        return scanResult.getClassesWithAnnotation(annotation.getName()).loadClasses();
    }
}
