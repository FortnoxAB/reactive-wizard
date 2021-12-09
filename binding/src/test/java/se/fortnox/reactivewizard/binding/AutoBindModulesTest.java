package se.fortnox.reactivewizard.binding;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.other.vendor.*;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.Test;
import org.springframework.stereotype.Component;
import se.fortnox.reactivewizard.binding.scanners.ClassScanner;

import static org.assertj.core.api.Assertions.assertThat;

public class AutoBindModulesTest {

    @Test
    public void shouldRunAutoBindModules() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        InjectedInTest instance = injector.getInstance(InjectedInTest.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_AUTO_BIND_MODULE);
    }

    @Test
    public void shouldBindImplementationsToTheirInterfaces() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        TestInterface instance = injector.getInstance(TestInterface.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_IMPLEMENTATION);
    }

    @Test
    public void shouldUseBootStrapBindingOverDefault() {
        Injector injector = Guice.createInjector(new AutoBindModules(binder->{
            binder.bind(TestInterface.class).toInstance(() -> Source.FROM_BOOTSTRAP);
        }));
        TestInterface instance = injector.getInstance(TestInterface.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_BOOTSTRAP);
    }

    @Test
    public void shouldUseHigherPrioModule() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        InjectedInTestPrio instance = injector.getInstance(InjectedInTestPrio.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_AUTO_BIND_MODULE_HIGH_PRIO);
    }

    @Test
    public void shouldSortByNameForSamePrioModule() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        InjectedInTestSamePrio instance = injector.getInstance(InjectedInTestSamePrio.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_AUTO_BIND_MODULE_MED2);
    }

    @Test
    public void shouldFindClassAnnotatedWithDeprecated() {
        ClassScanner classScanner = new AutoBindModules(){
            @Override
            public ClassScanner getClassScanner() {
                return super.getClassScanner();
            }
        }.getClassScanner();
        Iterable<Class<?>> foundClasses = classScanner.findClassesAnnotatedWith(Component.class);

        assertThat(foundClasses).isNotNull();
    }

}
