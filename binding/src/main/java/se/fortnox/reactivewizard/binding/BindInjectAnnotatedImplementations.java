package se.fortnox.reactivewizard.binding;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import jakarta.inject.Singleton;
import se.fortnox.reactivewizard.binding.scanners.InjectAnnotatedScanner;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Binds all @{@link jakarta.inject.Inject} annotated classes to it's defined interfaces.
 */
@Singleton
public class BindInjectAnnotatedImplementations implements AutoBindModule {
    private final InjectAnnotatedScanner injectAnnotatedScanner;

    @Inject
    public BindInjectAnnotatedImplementations(InjectAnnotatedScanner injectAnnotatedScanner) {
        this.injectAnnotatedScanner = injectAnnotatedScanner;
    }

    @Override
    public void configure(Binder binder) {
        Map<Type, Class<?>> configuredInterfaces = new HashMap<>();
        injectAnnotatedScanner.getClasses().forEach(injectAnnotated -> {
            for (Type iface : injectAnnotated.getGenericInterfaces()) {
                bindClassToInterface(binder, injectAnnotated, iface, configuredInterfaces);
            }
        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void bindClassToInterface(Binder binder, Class<?> cls, Type iface, Map<Type, Class<?>> configuredInterfaces) {
        if (Provider.class.isAssignableFrom(cls) || jakarta.inject.Provider.class.isAssignableFrom(cls)) {
            return;
        }

        if (!configuredInterfaces.containsKey(iface)) {
            TypeLiteral ifaceType = TypeLiteral.get(iface);
            binder.bind(ifaceType).to(cls);
            configuredInterfaces.put(iface, cls);
        }
    }

    @Override
    public Integer getPrio() {
        // Lower than the default, so that any normal module can override these bindings.
        return 50;
    }
}
