package se.fortnox.reactivewizard.binding.scanners;

import se.fortnox.reactivewizard.config.Config;

import javax.inject.Singleton;

/**
 * Finds all classes annotated with @{@link Config}.
 */
@Singleton
public class ConfigClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ClassScanner classScanner) {
        classScanner.findClassesAnnotatedWith(Config.class).forEach(this::add);
    }
}
