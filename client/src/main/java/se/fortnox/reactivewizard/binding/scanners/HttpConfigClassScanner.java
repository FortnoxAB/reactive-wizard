package se.fortnox.reactivewizard.binding.scanners;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.config.Config;

import javax.inject.Singleton;

@Singleton
/*
 * Finds all classes having a Config annotation and is a subclass of HttpClientConfig
 */
public class HttpConfigClassScanner extends AbstractClassScanner {
    @Override
    public void visit(FastClasspathScanner classpathScanner) {
        classpathScanner.matchClassesWithAnnotation(Config.class, classWithAnnotation -> {
            if (HttpClientConfig.class.isAssignableFrom(classWithAnnotation)) {
                this.add(classWithAnnotation);
            }
        });
    }
}
