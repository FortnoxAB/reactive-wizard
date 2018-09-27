package se.fortnox.reactivewizard.binding;

import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.annotation.PreDestroy;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PreDestroyModuleTest {
    @Mock
    private Runtime runtime;

    private Injector injector;

    @Before
    public void setUp() {
        injector = Guice.createInjector(new PreDestroyModule(runtime));
    }

    @Test
    public void shouldRegisterShutdownHook() {
        injector.getInstance(TestModule.class);
        verify(runtime).addShutdownHook(any());
    }

    @Test
    public void shouldCallAnnotatedMethod() {
        PreDestroyModule.PreDestroyCallbacks callbacks = injector.getInstance(PreDestroyModule.PreDestroyCallbacks.class);
        TestModule test = injector.getInstance(TestModule.class);
        callbacks.run();
        assertThat(test.preDestroyCalled).isTrue();
    }

    @Test
    public void shouldIgnoreUncheckedExceptions() {
        PreDestroyModule.PreDestroyCallbacks callbacks = injector.getInstance(PreDestroyModule.PreDestroyCallbacks.class);
        injector.getInstance(ExceptionThrowingTestModule.class);
        try {
            callbacks.run();
        } catch (Exception e) {
            fail("Should not throw exceptions");
        }
    }

    @Test(expected = ConfigurationException.class)
    public void shouldFailOnMethodWithArgument() {
        injector.getInstance(MethodWithArgumentTestModule.class);
    }

    private static class TestModule {
        boolean preDestroyCalled = false;

        @PreDestroy
        public void preDestroy() {
            preDestroyCalled = true;
        }
    }

    private static class ExceptionThrowingTestModule {
        @PreDestroy
        public void preDestroy() {
            throw new RuntimeException("testing @PreDestroy with exceptions");
        }
    }

    private static class MethodWithArgumentTestModule {
        @PreDestroy
        public void preDestroy(String foo) {
        }
    }
}