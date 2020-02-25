package se.fortnox.reactivewizard.dbmigrate;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import rx.Observable;
import rx.observers.AssertableSubscriber;
import se.fortnox.reactivewizard.binding.AutoBindModules;
import se.fortnox.reactivewizard.binding.ClassScannerImpl;
import se.fortnox.reactivewizard.binding.scanners.DaoClassScanner;
import se.fortnox.reactivewizard.db.ConnectionProviderImpl;
import se.fortnox.reactivewizard.db.DbProxy;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.statement.MinimumAffectedRowsException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import static se.fortnox.reactivewizard.binding.AutoBindModules.JAR_BLACKLIST;
import static se.fortnox.reactivewizard.binding.AutoBindModules.PATH_BLACKLIST;
import static se.fortnox.reactivewizard.test.TypeRandomizer.getType;

/**
 * Subclass this class and create a single test calling the method DaoTester#testDaoClasses
 * Then that test will scan your module and find any dao class specified and test all the sql-syntax
 */
public class DaoTester {

    private static Logger              LOG                 = LoggerFactory.getLogger(DaoTester.class);

    @ClassRule
    public static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer();

    private static LiquibaseMigrate    migrator;

    @BeforeClass
    public static void before() {
        migrate(postgreSQLContainer);
    }

    @After
    public void afterEach() throws Exception {
        migrator.forceDrop();
    }

    @Before
    public void beforeEach() throws Exception {
        migrator.run();
    }

    @Test
    public void testDaoClasses() throws Exception {
        DatabaseConfig databaseConfig = setupDatabaseConfig(postgreSQLContainer);

        DbProxy dbProxy = new DbProxy(databaseConfig, new ConnectionProviderImpl(databaseConfig));

        for (Class<?> aClass : this.getClasses()) {
            Object instance = dbProxy.create(aClass);

            Method[] methods = aClass.getMethods();

            for (Method method : methods) {
                testDaoMethod(aClass, method, instance);
            }
        }
    }

    /**
     * Executing the desired class and method and asserting the query syntax is ok
     * @param daoClass The Class we are testing
     * @param method The method we are testing
     * @param daoInstance the instance of the class
     */
    private void testDaoMethod(Class daoClass, Method method, Object daoInstance) throws Exception {

        LOG.info("Testing class {} and method {}", daoClass, method.getName());
        ArrayList<Object> mockValues = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
            mockValues.add(modify(getRandomValue(parameter.getType())));
        }

        AssertableSubscriber<?> test = ((Observable<?>)method.invoke(daoInstance, mockValues.toArray())).test().awaitTerminalEvent();

        for (Throwable onErrorEvent : test.getOnErrorEvents()) {
            if (!isMinimumEffectedRowsException(onErrorEvent) && !isDuplicateKey(onErrorEvent)) {
                LOG.error("Query should not fail", onErrorEvent);
                Assert.fail("Query should not fail on class " + daoClass + " and method " + method.getName() + " with error " + onErrorEvent.getCause());
            }
        }
    }

    private boolean isMinimumEffectedRowsException(Throwable onErrorEvent) {
        return (onErrorEvent.getCause() instanceof MinimumAffectedRowsException);
    }

    private boolean isDuplicateKey(Throwable onErrorEvent) {
        return onErrorEvent.getCause() != null && onErrorEvent.getCause().getMessage() != null && onErrorEvent.getCause().getMessage().contains("duplicate key value violates unique constraint");
    }
    /**
     * Override to customize random parameter generation
     *
     * Sometimes legacy columns are strings but still expected to contain numbers, here you can customize that.
     *
     * @param object the object created
     *
     * @return randomized value
     */
    protected Object modify(Object object) {
        return object;
    }


    private Object getRandomValue(Class<?> type) throws Exception {
        return getType(type);
    }

    /**
     * Scans the classpath for dao-classes that we can test
     * @return all dao classes
     */
    private Iterable<Class<?>> getClasses() {

        ClassGraph classGraph      = new ClassGraph()
            .blacklistPackages(AutoBindModules.getPackageBlacklist())
            .blacklistJars(JAR_BLACKLIST)
            .blacklistPaths(PATH_BLACKLIST)
            .whitelistPackages()
            .enableMethodInfo()
            .enableAnnotationInfo();
        ScanResult scanResult = classGraph.scan();

        DaoClassScanner daoClassScanner = new DaoClassScanner();
        daoClassScanner.visit(new ClassScannerImpl(scanResult));
        return daoClassScanner.getClasses();
    }

    private static void migrate(PostgreSQLContainer postgreSQLContainer) {
        migrator = getMigrator(setupDatabaseConfig(postgreSQLContainer));
    }

    @NotNull
    private static LiquibaseMigrate getMigrator(LiquibaseConfig databaseConfig) {
        try {
            LiquibaseMigrate       liquibaseMigrate = new LiquibaseMigrate(databaseConfig);
            final Enumeration<URL> urlIterator      = DaoTester.class.getClassLoader().getResources(databaseConfig.getMigrationsFile());

            while(urlIterator.hasMoreElements()) {
                LOG.info("Found migration here {}", urlIterator.nextElement().getFile());
            }

            return liquibaseMigrate;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private static LiquibaseConfig setupDatabaseConfig(PostgreSQLContainer postgreSQLContainer) {
        LiquibaseConfig databaseConfig = new LiquibaseConfig();

        databaseConfig.setUrl(postgreSQLContainer.getJdbcUrl());
        databaseConfig.setUser(postgreSQLContainer.getUsername());
        databaseConfig.setPassword(postgreSQLContainer.getPassword());
        databaseConfig.setPoolSize(1);

        return databaseConfig;
    }
}
