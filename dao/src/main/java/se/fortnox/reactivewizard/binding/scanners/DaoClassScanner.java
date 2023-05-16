package se.fortnox.reactivewizard.binding.scanners;

import jakarta.inject.Singleton;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.Update;

@Singleton
public class DaoClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ClassScanner classScanner) {
        classScanner.findClassesWithMethodAnnotation(Query.class).forEach(this::addIfInterface);
        classScanner.findClassesWithMethodAnnotation(Update.class).forEach(this::addIfInterface);
    }

    private void addIfInterface(Class cls) {
        if (cls.isInterface()) {
            this.add(cls);
        }
    }
}
