package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import javax.inject.Singleton;
import javax.ws.rs.Path;

@Singleton
public class JaxRsClassScanner extends AbstractClassScanner {
    @Override
    public void visit(FastClasspathScanner fastClasspathScanner) {
        fastClasspathScanner.matchClassesWithAnnotation(Path.class, cls->{
            if (cls.isInterface()) {
                add(cls);
            }
        });
    }
}
