package se.fortnox.reactivewizard.binding.scanners;

import se.fortnox.reactivewizard.client.HttpClientConfig;
import se.fortnox.reactivewizard.config.Config;

import javax.inject.Singleton;

@Singleton
/*
 * Finds all classes having a Config annotation and is a subclass of HttpClientConfig
 */
public class HttpConfigClassScanner extends AbstractClassScanner {
    @Override
    public void visit(ClassScanner classScanner) {
        classScanner.findClassesAnnotatedWith(Config.class).forEach(cls -> {
            if (HttpClientConfig.class.isAssignableFrom(cls)) {
                this.add(cls);
            }
        });
    }
}
