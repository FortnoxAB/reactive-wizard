package se.fortnox.reactivewizard.binding.scanners;

import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.Update;

import javax.inject.Singleton;

@Singleton
public class DaoClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ScanResult scanResult) {
        scanResult.getClassesWithMethodAnnotation(Query.class.getName()).forEach(this::addIfInterface);
        scanResult.getClassesWithMethodAnnotation(Update.class.getName()).forEach(this::addIfInterface);
    }

    private void addIfInterface(ClassInfo classInfo) {
        if (classInfo.isInterface()) {
            this.add(classInfo.loadClass());
        }
    }
}
