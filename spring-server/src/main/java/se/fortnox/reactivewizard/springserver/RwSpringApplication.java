package se.fortnox.reactivewizard.springserver;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Needed to create the application context at startup of the injector and run it at a later time when the spring configuration classes can depend on rw classes
 */
public class RwSpringApplication extends SpringApplication {
    private static ConfigurableApplicationContext applicationContext;
    private static RwSpringApplication            instance;

    private RwSpringApplication(Class<?> configClass) {
        super(configClass);
    }

    public static RwSpringApplication getInstance(Class<?> configClass) {
        if (instance == null) {
            instance = new RwSpringApplication(configClass);
        }
        return instance;
    }

    @Override
    public ConfigurableApplicationContext createApplicationContext() {
        if (applicationContext == null) {
            applicationContext = super.createApplicationContext();
        }
        return applicationContext;
    }
}
