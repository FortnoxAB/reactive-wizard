package se.fortnox.reactivewizard.binding.scanners;

import io.github.classgraph.ScanResult;
import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.config.Config;

import javax.inject.Singleton;

@Singleton
/*
 * Finds all classes having a Config annotation and is a subclass of HttpClientConfig
 */
public class HttpConfigClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ScanResult scanResult) {
        scanResult.getClassesWithAnnotation(Config.class.getName()).forEach(classInfo -> {
            Class<?> cls = classInfo.loadClass();
            if (HttpClientConfig.class.isAssignableFrom(cls)) {
                this.add(cls);
            }
        });
    }
}
