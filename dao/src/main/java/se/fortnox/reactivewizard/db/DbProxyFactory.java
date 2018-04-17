package se.fortnox.reactivewizard.db;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.binding.scanners.DaoClassScanner;

import javax.inject.Inject;

public class DbProxyFactory implements AutoBindModule {

    private final DaoClassScanner daoClassScanner;

    @Inject
    public DbProxyFactory(DaoClassScanner daoClassScanner) {
        this.daoClassScanner = daoClassScanner;
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
}
