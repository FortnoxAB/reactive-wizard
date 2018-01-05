package se.fortnox.reactivewizard.binding;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import se.fortnox.reactivewizard.binding.scanners.AbstractClassScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Guice module which does automatic binding of all classes.
 * Implement AutoBindModule to add your own bindings, or just add @Inject to
 * your constructor to get an implicit binding.
 *
 * Finds all {@link AutoBindModule} implementations and merges them into a single module.
 */
public class AutoBindModules implements Module {

    private static final String[] PACKAGE_BLACKLIST = {
            "-com.google",
            "-liquibase",
            "-io.netty",
            "-org.yaml",
            "-com.fasterxml",
            "-org.postgresql",
            "-org.apache",
            "-org.hibernate",
            "-rx",
            "-org.jetbrains",
            "-com.intellij",
            "-com.netflix",
            "-io.reactivex",
            "-com.sun",
            "-com.codahale",
            "-com.zaxxer",
            "-io.github.lukehutch",
            "-io.prometheus",
            "-org.jboss",
            "-org.aopalliance",
            "-redis",
            "-net.minidev",
            "-org.slf4j",
            "-META-INF",
            "-com/ryantenney",
            "-net/logstash",
            "-jar:java-atk-wrapper.jar",
            "-jar:rt.jar",
            "-jar:idea_rt.jar"
    };
    private Module bootstrapBindings;

    public AutoBindModules() {
        this(binder -> { });
    }

    public AutoBindModules(Module bootstrapBindings) {
        this.bootstrapBindings = bootstrapBindings;
    }

    @Override
    public void configure(Binder binder) {
        Injector bootstrapInjector = Guice.createInjector(bootstrapBindings);

        List<AutoBindModule> autoBindModules = createAutoBindModules(bootstrapInjector);

        // Sort important due to overrides below
        Collections.sort(autoBindModules);

        Module mergedModule = null;

        for (AutoBindModule m : autoBindModules) {
            if (mergedModule == null) {
                mergedModule = m;
            } else {
                mergedModule = Modules.override(mergedModule).with(m);
            }
            m.preBind();
        }

        mergedModule = Modules.override(mergedModule)
                .with(bootstrapBindings);

        binder.disableCircularProxies();
        mergedModule.configure(binder);
    }

    private List<AutoBindModule> createAutoBindModules(Injector bootstrapInjector) {
        List<AbstractClassScanner> scanners = createScanners(bootstrapInjector);

        FastClasspathScanner classpathScanner = new FastClasspathScanner(PACKAGE_BLACKLIST);
        List<Class<? extends AutoBindModule>> autoBindModuleClasses = new ArrayList<>();
        scanners.forEach(scanner -> scanner.visit(classpathScanner));
        classpathScanner.matchClassesImplementing(AutoBindModule.class, autoBindModuleClasses::add);
        classpathScanner.scan();
        return autoBindModuleClasses.stream().map(bootstrapInjector::getInstance).collect(Collectors.toList());
    }

    private List<AbstractClassScanner> createScanners(Injector factoryInjector) {
        FastClasspathScanner classpathScanner = new FastClasspathScanner(AbstractClassScanner.class.getPackage().getName());
        List<Class<? extends AbstractClassScanner>> scanners = new ArrayList<>();
        classpathScanner.matchSubclassesOf(AbstractClassScanner.class, scanners::add);
        classpathScanner.scan();
        return scanners.stream().map(factoryInjector::getInstance).collect(Collectors.toList());
    }
}
