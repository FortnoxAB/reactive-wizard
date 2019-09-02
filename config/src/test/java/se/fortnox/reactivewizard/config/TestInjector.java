package se.fortnox.reactivewizard.config;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.apache.commons.lang.ArrayUtils;
import se.fortnox.reactivewizard.binding.AutoBindModules;

import javax.annotation.Nullable;
import java.util.function.Consumer;

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
                    bind(String[].class).annotatedWith(Names.named("args")).toInstance((String[])ArrayUtils.addAll(args, new String[]{configFile}));
                }

                binderConsumer.accept(binder());
            }
        };

        return Guice.createInjector(new AutoBindModules(module));
    }
}
