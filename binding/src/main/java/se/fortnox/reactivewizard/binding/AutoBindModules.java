package se.fortnox.reactivewizard.binding;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import se.fortnox.reactivewizard.binding.scanners.AbstractClassScanner;
import se.fortnox.reactivewizard.binding.scanners.ClassScanner;

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
    public static final List<String> PACKAGE_BLACKLIST = ImmutableList.of(
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
        "io.github.classgraph",
        "io.prometheus",
        "org.jboss",
        "org.aopalliance",
        "redis",
        "net.minidev",
        "org.slf4j",
        "com.ryantenney",
        "net.logstash"
        );
    public static final String[] JAR_BLACKLIST = {
        "java-atk-wrapper.jar",
        "rt.jar",
        "idea_rt.jar"
    };
    public static final String[] PATH_BLACKLIST = {
        "META-INF"
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

            try (ClassScanner classScanner = getClassScanner()) {
                List<AbstractClassScanner> scanners = createScanners(classScanner, bootstrapInjector);
                scanners.forEach(scanner -> scanner.visit(classScanner));
                classScanner.findClassesImplementing(AutoBindModule.class).forEach(autoBindModuleClasses::add);
            }
            autoBindModules = autoBindModuleClasses.stream().map(bootstrapInjector::getInstance).collect(Collectors.toList());
        }
        return autoBindModules;
    }

    protected ClassScanner getClassScanner() {
        ClassGraph classGraph      = new ClassGraph()
                .blacklistPackages(getPackageBlacklist())
                .blacklistJars(JAR_BLACKLIST)
                .blacklistPaths(PATH_BLACKLIST)
                .whitelistPackages(getPackageWhitelist())
                .enableMethodInfo()
                .enableAnnotationInfo();
        ScanResult scanResult = classGraph.scan();
        return new ClassScannerImpl(scanResult);
    }

    protected String[] getPackageWhitelist() {
        return new String[0];
    }

    public static String[] getPackageBlacklist() {
        return PACKAGE_BLACKLIST.toArray(new String[0]);
    }

    private List<AbstractClassScanner> createScanners(ClassScanner classScanner, Injector factoryInjector) {
        List<Class<? extends AbstractClassScanner>> scanners = new ArrayList<>();
        classScanner.findSubclassesOf(AbstractClassScanner.class)
            .forEach(scanners::add);
        return scanners.stream().map(factoryInjector::getInstance).collect(Collectors.toList());
    }
}
