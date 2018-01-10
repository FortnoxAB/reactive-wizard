package se.fortnox.reactivewizard;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;
import se.fortnox.reactivewizard.binding.AutoBindModules;

public class Main {
    public static void main(String[] args) {

        try {
            AutoBindModules autoBindModules = new AutoBindModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String[].class).annotatedWith(Names.named("args")).toInstance(args);
                }
            });
            Guice.createInjector(autoBindModules);
        } catch (Throwable t) {
            if (t.getCause() != null) {
                t.getCause().printStackTrace();
            }
            t.printStackTrace();
            throw t;
        }
    }
}