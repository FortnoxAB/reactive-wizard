package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.Update;

import javax.inject.Singleton;
import java.lang.reflect.Executable;

@Singleton
public class DaoClassScanner extends AbstractClassScanner {
    @Override
    public void visit(FastClasspathScanner fastClasspathScanner) {
        fastClasspathScanner.matchClassesWithMethodAnnotation(Query.class, this::addIfInteface);
        fastClasspathScanner.matchClassesWithMethodAnnotation(Update.class, this::addIfInteface);
    }

    private void addIfInteface(Class<?> cls, Executable executable) {
        if (cls.isInterface()) {
            this.add(cls);
        }
    }
}
