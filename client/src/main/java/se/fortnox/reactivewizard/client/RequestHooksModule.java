package se.fortnox.reactivewizard.client;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class RequestHooksModule implements AutoBindModule {

    @Override
    public void configure(Binder binder) {
        Multibinder.newSetBinder(binder, TypeLiteral.get(PreRequestHook.class));
    }
}
