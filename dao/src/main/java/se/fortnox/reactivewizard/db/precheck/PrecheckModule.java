package se.fortnox.reactivewizard.db.precheck;

import com.google.inject.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.binding.AutoBindModule;

import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Singleton
public class PrecheckModule implements AutoBindModule {
    private static Logger LOG = LoggerFactory.getLogger(PrecheckModule.class);

    public PrecheckModule() {
        try {
            Method    method         = PrecheckModule.class.getMethod("configure", Binder.class);
            Parameter firstParameter = method.getParameters()[0];
            if (!firstParameter.isNamePresent()) {
                LOG.error("The project was not compiled with the -parameters option which is required for DAO parameter resolving. " +
                    "Please see https://github.com/FortnoxAB/reactive-wizard/wiki/CompilerParameters for more information.");
                System.exit(1);
            }
        } catch (NoSuchMethodException ignored) {
        }
    }

    @Override
    public void configure(Binder binder) {

    }

    @Override
    public Integer getPrio() {
        return 500;
    }
}
