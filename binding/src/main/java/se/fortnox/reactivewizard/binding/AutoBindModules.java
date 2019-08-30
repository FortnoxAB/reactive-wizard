package se.fortnox.reactivewizard.binding;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
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

    /**
     * These are packages that are not part of the application code and where there is no point to scan for classes.
     * We only want to find classes defined by our platform and user code.
     */
    private static final String[] PACKAGE_BLACKLIST = {
        "com.google",
        "liquibase",
        "io.netty",
        "org.yaml",
        "com.fasterxml",
        "org.postgresql",
        "org.apache",
        "org.hibernate",
        "rx",
        "org.jetbrains",
        "com.intellij",
        "com.netflix",
        "io.reactivex",
        "com.sun",
        "com.codahale",
        "com.zaxxer",
        "io.github.lukehutch",
        "io.prometheus",
        "org.jboss",
        "org.aopalliance",
        "redis",
        "net.minidev",
        "org.slf4j",
        "com.ryantenney",
        "net.logstash",
        "META-INF",
        "jar:java-atk-wrapper.jar",
        "jar:rt.jar",
        "jar:idea_rt.jar"
    };
    private              Module   bootstrapBindings;
    private static List<AutoBindModule> autoBindModules;

    public AutoBindModules() {
        this(binder -> {
        });
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
        if (autoBindModules == null) {
            // We cache the auto bind modules in a static variable, since they will not change during execution, as they
            // are a result of the current class path.
            List<Class<? extends AutoBindModule>> autoBindModuleClasses = new ArrayList<>();
            List<AbstractClassScanner> scanners = createScanners(bootstrapInjector);

            ClassGraph classGraph      = new ClassGraph()
                .blacklistPackages(PACKAGE_BLACKLIST)
                .enableMethodInfo()
                .enableAnnotationInfo();
            try (ScanResult scanResult = classGraph.scan()) {
                scanners.forEach(scanner -> scanner.visit(scanResult));
                scanResult.getClassesImplementing(AutoBindModule.class.getName()).stream()
                    .map(ci -> ci.loadClass(AutoBindModule.class))
                    .forEach(autoBindModuleClasses::add);
            }
            autoBindModules = autoBindModuleClasses.stream().map(bootstrapInjector::getInstance).collect(Collectors.toList());
        }
        return autoBindModules;
    }

    private List<AbstractClassScanner> createScanners(Injector factoryInjector) {
        ClassGraph classGraph = new ClassGraph().whitelistPackages(AbstractClassScanner.class.getPackage().getName());
        List<Class<? extends AbstractClassScanner>> scanners = new ArrayList<>();
        try (ScanResult scanResult = classGraph.scan()) {
            scanResult.getSubclasses(AbstractClassScanner.class.getName()).stream()
                .map(ci -> ci.loadClass(AbstractClassScanner.class))
                .forEach(scanners::add);
        }
        return scanners.stream().map(factoryInjector::getInstance).collect(Collectors.toList());
    }
}
