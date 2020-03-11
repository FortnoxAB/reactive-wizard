package se.fortnox.reactivewizard.jaxrs.context;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import io.reactiverse.reactivecontexts.core.Context;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.jaxrs.JaxRsResourceInterceptor;

public class JaxRsRequestContextModule implements AutoBindModule {
    @Override
    public void configure(Binder binder) {
        Context.load();

        Multibinder.newSetBinder(binder, JaxRsResourceInterceptor.class)
            .addBinding().to(JaxRsRequestContextInterceptor.class);
    }
}
