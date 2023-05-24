package se.fortnox.reactivewizard.binding;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.other.vendor.InjectedInTest;
import com.other.vendor.InjectedInTestPrio;
import com.other.vendor.InjectedInTestSamePrio;
import com.other.vendor.Source;
import com.other.vendor.TestInterface;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutoBindModulesTest {

    @Test
    void shouldRunAutoBindModules() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        InjectedInTest instance = injector.getInstance(InjectedInTest.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_AUTO_BIND_MODULE);
    }

    @Test
    void shouldBindImplementationsToTheirInterfaces() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        TestInterface instance = injector.getInstance(TestInterface.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_IMPLEMENTATION);
    }

    @Test
    void shouldUseBootStrapBindingOverDefault() {
        Injector injector = Guice.createInjector(new AutoBindModules(binder->{
            binder.bind(TestInterface.class).toInstance(() -> Source.FROM_BOOTSTRAP);
        }));
        TestInterface instance = injector.getInstance(TestInterface.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_BOOTSTRAP);
    }

    @Test
    void shouldUseHigherPrioModule() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        InjectedInTestPrio instance = injector.getInstance(InjectedInTestPrio.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_AUTO_BIND_MODULE_HIGH_PRIO);
    }

    @Test
    void shouldSortByNameForSamePrioModule() {
        Injector injector = Guice.createInjector(new AutoBindModules());
        InjectedInTestSamePrio instance = injector.getInstance(InjectedInTestSamePrio.class);
        assertThat(instance.getSource()).isEqualTo(Source.FROM_AUTO_BIND_MODULE_MED2);
    }

}
