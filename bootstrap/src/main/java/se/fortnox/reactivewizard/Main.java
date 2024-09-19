package se.fortnox.reactivewizard;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.binding.AutoBindModules;

/**
 * Main application entry point. Will scan for all modules on the classpath and run them.
 * If you want to run code at startup, use a custom module.
 * <p>
 * Requires a config file as last parameter.
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    /**
     * Main application entry point.
     *
     * @param args Commandline arguments
     */
    public static void main(String[] args) {
        try {
            Module bootstrap = new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String[].class).annotatedWith(Names.named("args")).toInstance(args);
                }
            };
            Guice.createInjector(new AutoBindModules(bootstrap));
        } catch (Exception exception) {
            LOG.error("Caught exception at startup.", exception);
            System.exit(-1);
        }
    }
}
