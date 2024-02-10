package se.fortnox.reactivewizard.db;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.db.statement.DbStatementFactoryFactory;
import se.fortnox.reactivewizard.db.transactions.ConnectionScheduler;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.DebugUtil;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.text.MessageFormat.format;

@Singleton
public class DbProxy implements InvocationHandler {

    private static final TypeReference<Object[]> OBJECT_ARRAY_TYPE_REFERENCE = new TypeReference<>() {
    };
    private final DbStatementFactoryFactory dbStatementFactoryFactory;
    private final Scheduler scheduler;
    protected final Map<Method, ReactiveStatementFactory> statementFactories;
    private final ConnectionScheduler connectionScheduler;
    protected final Function<Object[], String> paramSerializer;
    private final DatabaseConfig databaseConfig;

    @Inject
    public DbProxy(DatabaseConfig databaseConfig,
                   @Nullable ConnectionProvider connectionProvider,
                   DbStatementFactoryFactory dbStatementFactoryFactory,
                   JsonSerializerFactory jsonSerializerFactory
    ) {
        this(databaseConfig,
                threadPool(databaseConfig.getPoolSize()),
                connectionProvider,
                dbStatementFactoryFactory,
                jsonSerializerFactory);
    }

    public DbProxy(DatabaseConfig databaseConfig,
                   Scheduler scheduler,
                   ConnectionProvider connectionProvider,
                   DbStatementFactoryFactory dbStatementFactoryFactory,
                   JsonSerializerFactory jsonSerializerFactory
    ) {
        this(databaseConfig, scheduler, connectionProvider, dbStatementFactoryFactory,
                jsonSerializerFactory.createStringSerializer(OBJECT_ARRAY_TYPE_REFERENCE),
                new ConcurrentHashMap<>());
    }

    public DbProxy(DatabaseConfig databaseConfig, ConnectionProvider connectionProvider) {
        this(databaseConfig,
                Schedulers.boundedElastic(),
                connectionProvider,
                new DbStatementFactoryFactory(),
                new JsonSerializerFactory());
    }

    protected DbProxy(DatabaseConfig databaseConfig,
                      Scheduler scheduler,
                      ConnectionProvider connectionProvider,
                      DbStatementFactoryFactory dbStatementFactoryFactory,
                      Function<Object[], String> paramSerializer,
                      Map<Method, ReactiveStatementFactory> statementFactories
    ) {
        this.scheduler = scheduler;
        this.dbStatementFactoryFactory = dbStatementFactoryFactory;
        this.paramSerializer = paramSerializer;
        this.databaseConfig = databaseConfig;
        this.statementFactories = statementFactories;
        this.connectionScheduler = new ConnectionScheduler(connectionProvider, scheduler);
    }

    private static Scheduler threadPool(int poolSize) {
        if (poolSize == -1) {
            return Schedulers.boundedElastic();
        }
        return Schedulers.newBoundedElastic(10, Integer.MAX_VALUE, "DbProxy");
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
        ReactiveStatementFactory reactiveStatementFactory = statementFactories.get(method);
        if (reactiveStatementFactory == null || DebugUtil.IS_DEBUG) {
            if (DebugUtil.IS_DEBUG) {
                // Need to get the actual interface method in order to get updated annotations
                method = ReflectionUtil.getRedefinedMethod(method);
            }

            DbStatementFactory statementFactory = dbStatementFactoryFactory.createStatementFactory(method);
            PagingOutput pagingOutput = new PagingOutput(method);
            reactiveStatementFactory = new ReactiveStatementFactory(
                    statementFactory,
                    pagingOutput,
                    createMetrics(method),
                    databaseConfig,
                    method);
            statementFactories.put(method, reactiveStatementFactory);
        }

        return reactiveStatementFactory.create(args, connectionScheduler);
    }

    private Metrics createMetrics(Method method) {
        String type = method.isAnnotationPresent(Query.class) ? "query" : "update";
        String metricsName = format(
                "DAO_type:{0}_method:{1}.{2}_{3}",
                type, method.getDeclaringClass().getName(), method.getName(), method.getParameterCount());
        return Metrics.get(metricsName);
    }

    public DbProxy usingConnectionProvider(ConnectionProvider connectionProvider) {
        return new DbProxy(databaseConfig, scheduler, connectionProvider, dbStatementFactoryFactory, paramSerializer, statementFactories);
    }

    public DbProxy usingConnectionProvider(ConnectionProvider connectionProvider, DatabaseConfig databaseConfig) {
        return new DbProxy(databaseConfig, scheduler, connectionProvider, dbStatementFactoryFactory, paramSerializer, statementFactories);
    }

    public DbProxy usingConnectionProvider(ConnectionProvider newConnectionProvider, Scheduler newScheduler) {
        return new DbProxy(databaseConfig, newScheduler, newConnectionProvider, dbStatementFactoryFactory, paramSerializer, statementFactories);
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

}
