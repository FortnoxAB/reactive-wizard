package se.fortnox.reactivewizard.config;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.logging.LoggingShutdownHandler;

import javax.annotation.Nullable;
import java.util.function.Consumer;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.mockito.Mockito.mock;

public class TestInjector {

    public static Injector create() {
        return create(binder->{}, null);
    }

    public static Injector create(Consumer<Binder> binder) {
        return create(binder, null);
    }

    public static Injector create(String configFile) {
        return create(binder->{}, configFile);
    }

    public static Injector create(Consumer<Binder> binderConsumer, @Nullable String configFile) {
        return create(binderConsumer, configFile, new String[0]);
    }

    public static Injector create(Consumer<Binder> binderConsumer, @Nullable String configFile, String[] args) {
        Module module = new AbstractModule() {
            @Override
            protected void configure() {

                if (configFile == null) {
                    bind(ConfigFactory.class).toInstance(MockConfigFactory.create());
                    bind(String[].class).annotatedWith(Names.named("args")).toInstance(args);
                } else {
                    String[] argsWithConfig = concat(of(args), of(configFile)).toArray(String[]::new);
                    bind(String[].class).annotatedWith(Names.named("args")).toInstance(argsWithConfig);
                }

                bind(LoggingShutdownHandler.class).toInstance(mock(LoggingShutdownHandler.class));

                binderConsumer.accept(binder());
            }
        };

        return Guice.createInjector(new AutoBindModules(module));
    }
}
