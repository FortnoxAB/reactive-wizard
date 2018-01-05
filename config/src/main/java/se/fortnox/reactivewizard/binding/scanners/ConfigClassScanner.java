package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import se.fortnox.reactivewizard.config.Config;

import javax.inject.Singleton;

/**
 * Finds all classes annotated with @{@link Config}
 */
@Singleton
public class ConfigClassScanner extends AbstractClassScanner {
    @Override
    public void visit(FastClasspathScanner fastClasspathScanner) {
        fastClasspathScanner.matchClassesWithAnnotation(Config.class, this::add);
    }
}
