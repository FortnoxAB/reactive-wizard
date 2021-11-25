package se.fortnox.reactivewizard.springserver;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

public class MySpringApplication extends SpringApplication {
    private static ConfigurableApplicationContext applicationContext;
    private static MySpringApplication            instance;

    private MySpringApplication(Class<?> configClass) {
        super(configClass);
    }

    public static MySpringApplication getInstance(Class<?> configClass) {
        if (instance == null) {
            instance = new MySpringApplication(configClass);
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

    @Override
    public void load(ApplicationContext context, Object[] sources) {
        super.load(context, sources);
    }
}
