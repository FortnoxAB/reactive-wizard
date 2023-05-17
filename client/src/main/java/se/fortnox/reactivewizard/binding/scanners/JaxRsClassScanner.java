package se.fortnox.reactivewizard.binding.scanners;

import jakarta.inject.Singleton;

import javax.ws.rs.Path;

@Singleton
public class JaxRsClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ClassScanner classScanner) {
        classScanner.findClassesAnnotatedWith(Path.class).forEach(cls -> {
            if (cls.isInterface()) {
                add(cls);
            }
        });
    }
}
