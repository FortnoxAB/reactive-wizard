package se.fortnox.reactivewizard.springserver;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.guice.annotation.EnableGuiceModules;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.config.ConfigFactory;
import se.fortnox.reactivewizard.logging.LoggingFactory;

import java.nio.file.NoSuchFileException;

@EnableGuiceModules
@Configuration
public class RwStart {

    @Bean
    public AutoBindModules autoBindModules(ApplicationArguments applicationArguments) throws NoSuchFileException {
        ConfigFactory  configFactory  = createConfigFactory(applicationArguments.getSourceArgs());
        LoggingFactory loggingFactory = configFactory.get(LoggingFactory.class);
        loggingFactory.init();
        Module bootstrap = new AbstractModule() {
            @Override
            protected void configure() {
                bind(String[].class).annotatedWith(Names.named("args")).toInstance(applicationArguments.getSourceArgs());
                bind(ConfigFactory.class).toInstance(configFactory);
            }
        };

        return new AutoBindModules(bootstrap);
    }

    /**
     * Prepares a ConfigFactory before setting up Guice.
     * <p>
     * As logging can be part of the configuration file and the configuration file
     * could be missing, we have a side effect of setting up and initializing a LoggingFactory
     * that doesn't depend on ConfigFactory, if the configuration file is missing.
     *
     * @param args commandline arguments
     * @return A ConfigFactory based on a configuration file.
     * @throws NoSuchFileException If the configuration file cannot be found.
     */
    private static ConfigFactory createConfigFactory(String[] args) throws NoSuchFileException {
        try {
            return new ConfigFactory(args);
        } catch (RuntimeException runtimeException) {
            Throwable cause = runtimeException.getCause();
            if (cause != null && cause.getClass().isAssignableFrom(NoSuchFileException.class)) {
                LoggingFactory loggingFactory = new LoggingFactory();
                loggingFactory.init();
                throw (NoSuchFileException)cause;
            }
            throw runtimeException;
        }
    }

}
