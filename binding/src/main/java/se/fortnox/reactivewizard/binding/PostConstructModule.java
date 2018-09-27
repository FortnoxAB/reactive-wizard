package se.fortnox.reactivewizard.binding;

import com.google.inject.Binder;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.inject.matcher.Matchers.any;

/**
 * Finds methods annotated with {@link PostConstruct} and invokes them after injection is done.
 */
public class PostConstructModule implements AutoBindModule {
    private static final Logger LOG = LoggerFactory.getLogger(PostConstructModule.class);

    @Override
    public void configure(Binder binder) {
        binder.bindListener(any(), new PostConstructTypeListener());
    }

    private static class PostConstructTypeListener implements TypeListener {
        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            hear(type.getRawType(), encounter);
        }

        private <I> void hear(Class<? super I> type, TypeEncounter<I> encounter) {
            if (type == null || type.getPackage() == null || type.getPackage().getName().startsWith("java")) {
                return;
            }

            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    if (method.getParameterTypes().length != 0) {
                        encounter.addError("@PostConstruct annotated methods must not accept any argument");
                    }

                    encounter.register(new PostConstructInjectionListener<>(method));
                }
            }

            hear(type.getSuperclass(), encounter);
        }
    }

    private static class PostConstructInjectionListener<I> implements InjectionListener<I> {
        private final Method method;

        private PostConstructInjectionListener(Method method) {
            this.method = method;
        }

        @Override
        public void afterInjection(I injectee) {
            LOG.debug("Invoking @PostConstruct method {}", method);
            try {
                method.invoke(injectee);
            } catch (IllegalAccessException e) {
                throw new ProvisionException(String.format("Could not access @PostConstruct %s on %s",
                    method,
                    injectee), e);
            } catch (InvocationTargetException e) {
                throw new ProvisionException(String.format("Could not invoke @PostConstruct %s on %s",
                    method,
                    injectee), e.getTargetException());
            }
        }
    }
}
