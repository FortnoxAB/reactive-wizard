package se.fortnox.reactivewizard.config;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import se.fortnox.reactivewizard.binding.AutoBindModules;

import java.util.function.Consumer;

public class TestInjector {

    public static Injector create() {
        return create(binder->{});
    }

    public static Injector create(Consumer<Binder> binderConsumer) {
        return Guice.createInjector(new AutoBindModules(new AutoBindModules(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConfigFactory.class).toInstance(MockConfigFactory.create());
                bind(String[].class).annotatedWith(Names.named("args")).toInstance(new String[]{""});
                bind(ConfigAutoBindModule.class).to(MockConfigAutoBindModule.class);
                binderConsumer.accept(binder());
            }
        })));
    }

    private static final class MockConfigAutoBindModule extends ConfigAutoBindModule {

        public MockConfigAutoBindModule() {
            super(null, null);
        }

        @Override
        public void configure(Binder binder) {
        }
    }
}
