package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import se.fortnox.reactivewizard.config.Config;

import javax.inject.Singleton;

@Singleton
/*
 * Provides a list of all classes having a Config annotation
 */
public class ConfigClassScanner extends AbstractClassScanner {
    @Override
    public void visit(FastClasspathScanner classpathScanner) {
        classpathScanner.matchClassesWithAnnotation(Config.class, this::add);
    }
}
