package se.fortnox.reactivewizard.server;

import se.fortnox.reactivewizard.binding.scanners.AbstractClassScanner;
import se.fortnox.reactivewizard.binding.scanners.ClassScanner;

import javax.inject.Singleton;

/**
 * Finds all classes with @Inject annotated constructors.
 */
@Singleton
public class ServerConfigurerScanner extends AbstractClassScanner {
    @Override
    public void visit(ClassScanner classScanner) {
        classScanner.findClassesImplementing(ReactorServerConfigurer.class).forEach(this::add);
    }

}
