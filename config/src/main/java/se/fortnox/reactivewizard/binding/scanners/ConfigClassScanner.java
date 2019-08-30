package se.fortnox.reactivewizard.binding.scanners;

import io.github.classgraph.ScanResult;
import se.fortnox.reactivewizard.config.Config;

import javax.inject.Singleton;

/**
 * Finds all classes annotated with @{@link Config}.
 */
@Singleton
public class ConfigClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ScanResult scanResult) {
        scanResult.getClassesWithAnnotation(Config.class.getName()).loadClasses().forEach(this::add);
    }
}
