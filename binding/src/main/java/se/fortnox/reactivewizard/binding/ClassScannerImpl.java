package se.fortnox.reactivewizard.binding;

import io.github.classgraph.ScanResult;
import se.fortnox.reactivewizard.binding.scanners.ClassScanner;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.stream.Collectors;

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

    @Override
    public <T> Iterable<Class<? extends T>> findSubclassesOf(Class<T> parentClass) {
        return scanResult.getSubclasses(parentClass.getName())
            .stream()
            .map(classInfo -> classInfo.loadClass(parentClass))
            .collect(Collectors.toList());
    }

    @Override
    public <T> Iterable<Class<? extends T>> findClassesImplementing(Class<T> interfaceClass) {
        return scanResult.getClassesImplementing(interfaceClass.getName())
            .stream()
            .map(classInfo -> classInfo.loadClass(interfaceClass))
            .collect(Collectors.toList());
    }

    @Override
    public void close() {
        scanResult.close();
    }
}
