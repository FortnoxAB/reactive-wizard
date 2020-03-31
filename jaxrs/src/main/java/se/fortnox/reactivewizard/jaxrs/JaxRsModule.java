package se.fortnox.reactivewizard.jaxrs;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class JaxRsModule implements AutoBindModule {
    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, JaxRsResourceInterceptor.class);
    }
}
