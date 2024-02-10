package se.fortnox.reactivewizard.db;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactoryFactory;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.DebugUtil;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.text.MessageFormat.format;

@Singleton
public class DbProxy implements InvocationHandler {

    private final DbStatementFactoryFactory dbStatementFactoryFactory;
    protected final Map<Method, DaoMethodHandler> handlers;
    private final ReactiveStatementFactory reactiveStatementFactory;

    @Inject
    public DbProxy(ReactiveStatementFactory reactiveStatementFactory,
        DbStatementFactoryFactory dbStatementFactoryFactory) {
        this(reactiveStatementFactory, dbStatementFactoryFactory, new ConcurrentHashMap<>());
    }

    public DbProxy(DatabaseConfig databaseConfig, ConnectionProvider connectionProvider,
        DbStatementFactoryFactory dbStatementFactoryFactory) {
        this(new ReactiveStatementFactory(databaseConfig, connectionProvider), dbStatementFactoryFactory, new ConcurrentHashMap<>());
    }

    public DbProxy(DatabaseConfig databaseConfig, ConnectionProvider connectionProvider) {
        this(databaseConfig, Schedulers.boundedElastic(), connectionProvider, new DbStatementFactoryFactory());
    }

    public DbProxy(DatabaseConfig databaseConfig, Scheduler scheduler, ConnectionProvider connectionProvider,
        DbStatementFactoryFactory dbStatementFactoryFactory) {
        this(new ReactiveStatementFactory(databaseConfig, scheduler, connectionProvider), dbStatementFactoryFactory);
    }

    protected DbProxy(ReactiveStatementFactory reactiveStatementFactory,
        DbStatementFactoryFactory dbStatementFactoryFactory,
        Map<Method, DaoMethodHandler> handlers) {
        this.dbStatementFactoryFactory = dbStatementFactoryFactory;
        this.reactiveStatementFactory = reactiveStatementFactory;
        this.handlers = handlers;
    }

    /**
     * Create proxy from interface.
     *
     * @param daoInterface the interface
     * @param <T>          the type of the interface
     * @return the proxy
     */
    public <T> T create(Class<T> daoInterface) {
        return (T) Proxy.newProxyInstance(daoInterface.getClassLoader(),
            new Class[]{daoInterface},
            this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        var handler = handlers.get(method);
        if (handler == null || DebugUtil.IS_DEBUG) {
            if (DebugUtil.IS_DEBUG) {
                // Need to get the actual interface method in order to get updated annotations
                method = ReflectionUtil.getRedefinedMethod(method);
            }

            handler = new DaoMethodHandler(
                method,
                dbStatementFactoryFactory.createStatementFactory(method),
                new PagingOutput(method),
                createMetrics(method)
            );
            handlers.put(method, handler);
        }

        return handler.run(args, reactiveStatementFactory);
    }

    private Metrics createMetrics(Method method) {
        String type = method.isAnnotationPresent(Query.class) ? "query" : "update";
        String metricsName = format(
            "DAO_type:{0}_method:{1}.{2}_{3}",
            type, method.getDeclaringClass().getName(), method.getName(), method.getParameterCount());
        return Metrics.get(metricsName);
    }

    public DbProxy usingConnectionProvider(ConnectionProvider connectionProvider) {
        return new DbProxy(reactiveStatementFactory.usingConnectionProvider(connectionProvider), dbStatementFactoryFactory, handlers);
    }

    public DbProxy usingConnectionProvider(ConnectionProvider connectionProvider, DatabaseConfig databaseConfig) {
        return new DbProxy(reactiveStatementFactory.usingConnectionProvider(connectionProvider, databaseConfig), dbStatementFactoryFactory, handlers);
    }

    public DbProxy usingConnectionProvider(ConnectionProvider newConnectionProvider, Scheduler newScheduler) {
        return new DbProxy(reactiveStatementFactory.usingConnectionProvider(newConnectionProvider, newScheduler), dbStatementFactoryFactory, handlers);
    }

    public DatabaseConfig getDatabaseConfig() {
        return reactiveStatementFactory.getDatabaseConfig();
    }

}
