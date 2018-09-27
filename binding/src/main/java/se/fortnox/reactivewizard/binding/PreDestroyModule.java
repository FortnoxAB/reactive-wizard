package se.fortnox.reactivewizard.binding;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.google.inject.matcher.Matchers.any;

/**
 * Finds methods annotated with @{@link PreDestroy} and adds them to a shutdown hook.
 */
public class PreDestroyModule implements AutoBindModule {
    private static final Logger LOG = LoggerFactory.getLogger(PreDestroyModule.class);

    private final Runtime runtime;

    @Inject
    PreDestroyModule(Runtime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void configure(Binder binder) {
        final PreDestroyCallbacks callbacks = new PreDestroyCallbacks();
        binder.bind(PreDestroyCallbacks.class).toInstance(callbacks);
        binder.bindListener(any(), new PreDestroyTypeListener(callbacks));

        runtime.addShutdownHook(new Thread(callbacks));
    }

    static class PreDestroyCallbacks implements Runnable {
        private final List<Runnable> preDestroyCallbacks = new ArrayList<>();

        void add(Runnable runnable) {
            preDestroyCallbacks.add(runnable);
        }

        @Override
        public void run() {
            preDestroyCallbacks.forEach(Runnable::run);
        }
    }

    private static class PreDestroyTypeListener implements TypeListener {
        private final PreDestroyCallbacks callbacks;

        PreDestroyTypeListener(PreDestroyCallbacks callbacks) {
            this.callbacks = callbacks;
        }

        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            hear(type.getRawType(), encounter);
        }

        private <I> void hear(Class<? super I> type, TypeEncounter<I> encounter) {
            if (type == null || type.getPackage() == null || type.getPackage().getName().startsWith("java")) {
                return;
            }

            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    if (method.getParameterTypes().length != 0) {
                        encounter.addError("@PreDestroy annotated methods must not accept any argument");
                    }

                    encounter.register(injectionListener(method));
                }
            }

            hear(type.getSuperclass(), encounter);
        }

        private <I> InjectionListener<I> injectionListener(Method method) {
            return injectee -> callbacks.add(createCallback(injectee, method));
        }

        private Runnable createCallback(Object instance, Method method) {
            return () -> {
                LOG.debug("Invoking @PreDestroy method {}", method);
                try {
                    method.invoke(instance);
                } catch (IllegalAccessException e) {
                    LOG.warn(String.format("Could not access @PreDestroy %s on %s", method, instance), e);
                } catch (InvocationTargetException e) {
                    LOG.warn(String.format("Could not invoke @PreDestroy %s on %s", method, instance),
                        e.getTargetException());
                } catch (Exception e) {
                    LOG.warn(String.format("Could not invoke @PreDestroy %s on %s", method, instance), e);
                }
            };
        }
    }
}
