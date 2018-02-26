package se.fortnox.reactivewizard;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.name.Names;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.config.ConfigFactory;

/**
 * Main application entry point. Will scan for all modules on the classpath and run them.
 * If you want to run code at startup, use a custom module.
 *
 * Requires a config file as last parameter.
 */
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java -jar app.jar config.yml");
            return;
        }
        String configFile = args[args.length - 1];
        Module bootstrap = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConfigFactory.class).toInstance(new ConfigFactory(configFile));
                bind(String[].class).annotatedWith(Names.named("args")).toInstance(args);
            }
        };
        Guice.createInjector(new AutoBindModules(bootstrap));
    }
}