package se.fortnox.reactivewizard.binding.scanners;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Singleton
public class JaxRsClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ScanResult scanResult) {
        scanResult.getClassesWithAnnotation(Path.class.getName())
                .stream()
                .filter(ClassInfo::isInterface)
                .map(ClassInfo::loadClass)
                .forEach(this::add);
    }
}
