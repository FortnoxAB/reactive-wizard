package se.fortnox.reactivewizard.db;

import com.fasterxml.jackson.core.type.TypeReference;
import rx.Observable;
import rx.Scheduler;
import rx.internal.util.RxThreadFactory;
import rx.schedulers.Schedulers;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.db.statement.DbStatementFactoryFactory;
import se.fortnox.reactivewizard.db.transactions.TransactionStatement;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;
import se.fortnox.reactivewizard.metrics.Metrics;
import se.fortnox.reactivewizard.util.DebugUtil;
import se.fortnox.reactivewizard.util.FluxRxConverter;
import se.fortnox.reactivewizard.util.ReactiveDecorator;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Singleton
public class DbProxy implements InvocationHandler {

    private final DbStatementFactoryFactory               dbStatementFactoryFactory;
    private final Scheduler                               scheduler;
    private final Map<Method, ObservableStatementFactory> statementFactories;
    private final ConnectionProvider                      connectionProvider;
    private final Function<Object[], String>              paramSerializer;
    private final DatabaseConfig                          databaseConfig;

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
            jsonSerializerFactory.createStringSerializer(new TypeReference<Object[]>() {
            }),
            new ConcurrentHashMap<>());
    }

    public DbProxy(DatabaseConfig databaseConfig, ConnectionProvider connectionProvider) {
        this(databaseConfig,
            Schedulers.io(),
            connectionProvider,
            new DbStatementFactoryFactory(),
            new JsonSerializerFactory());
    }

    private DbProxy(DatabaseConfig databaseConfig,
        Scheduler scheduler,
        ConnectionProvider connectionProvider,
        DbStatementFactoryFactory dbStatementFactoryFactory,
        Function<Object[], String> paramSerializer,
        Map<Method, ObservableStatementFactory> statementFactories
    ) {
        this.scheduler = scheduler;
        this.connectionProvider = connectionProvider;
        this.dbStatementFactoryFactory = dbStatementFactoryFactory;
        this.paramSerializer = paramSerializer;
        this.databaseConfig = databaseConfig;
        this.statementFactories = statementFactories;
    }



    private static Scheduler threadPool(int poolSize) {
        if (poolSize == -1) {
            return Schedulers.io();
        }
        Executor executor = Executors.newFixedThreadPool(poolSize, new RxThreadFactory("DbProxy"));
        return Schedulers.from(executor);
    }

    public <T> T create(Class<T> daoInterface) {
        return (T)Proxy.newProxyInstance(daoInterface.getClassLoader(),
            new Class[]{daoInterface},
            this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        ObservableStatementFactory observableStatementFactory = statementFactories.get(method);
        if (observableStatementFactory == null || DebugUtil.IS_DEBUG) {
            if (DebugUtil.IS_DEBUG) {
                // Need to get the actual interface method in order to get updated annotations
                method = Optional.ofNullable(ReflectionUtil.getOverriddenMethod(method)).orElse(method);
            }
            DbStatementFactory statementFactory = dbStatementFactoryFactory.createStatementFactory(method);
            PagingOutput       pagingOutput     = new PagingOutput(method);
            observableStatementFactory = new ObservableStatementFactory(
                statementFactory,
                pagingOutput,
                scheduler,
                paramSerializer,
                createMetrics(method),
                databaseConfig);
            statementFactories.put(method, observableStatementFactory);
        }
        AtomicReference<TransactionStatement> transactionHolder = new AtomicReference<>();
        Observable<Object> resultObservable = observableStatementFactory.create(args, connectionProvider, transactionHolder);
        Class<?> returnType = method.getReturnType();
        Function<Observable<Object>,Object> converter = FluxRxConverter.converterFromObservable(returnType);
        return ReactiveDecorator.decorated(converter.apply(resultObservable), transactionHolder);
    }

    private Metrics createMetrics(Method method) {
        String type = method.isAnnotationPresent(Query.class) ? "query" : "update";
        return Metrics.get("DAO_type:" + type + "_method:" + method.getDeclaringClass().getName() + "." + method.getName() + "_" + method.getParameterCount());
    }

    public DbProxy usingConnectionProvider(ConnectionProvider connectionProvider) {
        return new DbProxy(databaseConfig, scheduler, connectionProvider, dbStatementFactoryFactory, paramSerializer, statementFactories);
    }

    public DbProxy usingConnectionProvider(ConnectionProvider connectionProvider, DatabaseConfig databaseConfig) {
        return new DbProxy(databaseConfig, scheduler, connectionProvider, dbStatementFactoryFactory, paramSerializer, statementFactories);
    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }
}
