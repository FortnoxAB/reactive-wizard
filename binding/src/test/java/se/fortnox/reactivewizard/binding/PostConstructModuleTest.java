package se.fortnox.reactivewizard.binding;

import com.google.inject.*;
import org.junit.Test;

import javax.annotation.PostConstruct;

import static org.fest.assertions.Assertions.assertThat;

public class PostConstructModuleTest {
    private Injector injector = Guice.createInjector(new PostConstructModule());

    @Test
    public void shouldCallMethodsAnnotatedWithPostConstruct() {
        TestModule test = injector.getInstance(TestModule.class);
        assertThat(test.constructed).isTrue();
    }

    @Test(expected = ProvisionException.class)
    public void shouldFailIfUncheckedExceptionIsThrown() {
        injector.getInstance(ExceptionThrowingTestModule.class);
    }

    @Test(expected = ConfigurationException.class)
    public void shouldFailOnMethodWithArgument() {
        injector.getInstance(MethodWithArgmuentTestModule.class);
    }

    private static class TestModule {
        private boolean constructed;

        @PostConstruct
        public void postConstruct() {
            constructed = true;
        }
    }

    private static class ExceptionThrowingTestModule {
        @PostConstruct
        public void postConstruct() {
            throw new RuntimeException("derp");
        }
    }

    private static class MethodWithArgmuentTestModule {
        @PostConstruct
        public void postConstruct(String foo) {
        }
    }
}