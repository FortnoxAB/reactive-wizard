package se.fortnox.reactivewizard.springserver;

import com.google.inject.Injector;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.guice.module.SpringModule;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class MappingSetup {

    @Inject
    public MappingSetup(@Named("args") String[] args, Injector injector) {

        AtomicReference<SpringApplication> springApplicationAtomicReference = new AtomicReference<>();

        injector.createChildInjector(new SpringModule(() -> {
            final MySpringApplication springApplication = new MySpringApplication(RwServerConfig.class);
            springApplicationAtomicReference.set(springApplication);
            return springApplication.createApplicationContext().getBeanFactory();
        }));

        springApplicationAtomicReference.get().run(args);
    }

    private static class MySpringApplication extends SpringApplication {
        private ConfigurableApplicationContext applicationContext;

        public MySpringApplication(Class<RwServerConfig> configClass) {
            super(configClass);
        }

        @Override
        public ConfigurableApplicationContext createApplicationContext() {
            if (applicationContext == null) {
                applicationContext = super.createApplicationContext();
            }
            return applicationContext;
        }
    }
}
