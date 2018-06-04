package se.fortnox.reactivewizard.db;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.DaoClassScanner;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class DbProxyFactory implements AutoBindModule {
    private static Logger LOG = LoggerFactory.getLogger(DbProxyFactory.class);

    private final DaoClassScanner daoClassScanner;

    @Inject
    public DbProxyFactory(DaoClassScanner daoClassScanner) {
        this.daoClassScanner = daoClassScanner;

        performCompilerCheck();
    }

    private <T> Provider<T> provider(Class<T> iface, Provider<DbProxy> dbProxyProvider) {
        return () -> dbProxyProvider.get().create(iface);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void configure(Binder binder) {
        Provider<DbProxy> dbProxyProvider = binder.getProvider(DbProxy.class);
        daoClassScanner.getClasses().forEach(cls -> {
            binder.bind((Class)cls)
                .toProvider(provider(cls, dbProxyProvider))
                .in(Scopes.SINGLETON);
        });
    }

    /**
     * Checks that we can access the name of a method parameter, and thereby
     * verifying that the compilation whas run with -parameters.
     */
    private void performCompilerCheck() {
        try {
            Method    method         = DbProxyFactory.class.getMethod("configure", Binder.class);
            Parameter firstParameter = method.getParameters()[0];
            if (!firstParameter.isNamePresent()) {
                LOG.error("The project was not compiled with the -parameters option which is required for DAO parameter resolving. " +
                    "Please see https://github.com/FortnoxAB/reactive-wizard/wiki/CompilerParameters for more information.");
                System.exit(1);
            }
        } catch (NoSuchMethodException ignored) {
        }
    }
}
