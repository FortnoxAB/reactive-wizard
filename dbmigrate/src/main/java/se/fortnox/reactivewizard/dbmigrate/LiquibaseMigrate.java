package se.fortnox.reactivewizard.dbmigrate;

import com.google.common.collect.Sets;
import liquibase.CatalogAndSchema;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.changelog.ChangeLogHistoryServiceFactory;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.executor.ExecutorService;
import liquibase.ext.TimeoutLockService;
import liquibase.lockservice.LockServiceFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import se.fortnox.reactivewizard.db.DbDriver;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Runs liquibase migrations for each file on the classpath named according to config. In a development environment this
 * is may be multiple files. In a production environment (fatjar) this is often a single file.
 */
public class LiquibaseMigrate {

    private List<Liquibase> liquibaseList;

    @Inject
    public LiquibaseMigrate(LiquibaseConfig liquibaseConfig) throws LiquibaseException, IOException {
        JdbcConnection conn = new JdbcConnection(getConnection(liquibaseConfig));

        Enumeration<URL> resources = this.getClass()
            .getClassLoader()
            .getResources(liquibaseConfig.getMigrationsFile());
        if (!resources.hasMoreElements()) {
            throw new RuntimeException("Could not find migrations file " + liquibaseConfig.getMigrationsFile());
        }

        TimeoutLockService.setRenewalConnectionCreator(() -> createDatabaseConnectionFromConfiguration(liquibaseConfig));

        liquibaseList = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL       url            = resources.nextElement();
            String    file           = url.toExternalForm();
            int       jarFileSep     = file.lastIndexOf('!');
            String    loggedFileName = file.substring(jarFileSep + 1);
            Liquibase liquibase      = new Liquibase(loggedFileName, new UrlAwareClassLoaderResourceAccessor(file), conn);
            liquibase.getDatabase().setDefaultSchemaName(liquibaseConfig.getSchema());

            liquibaseList.add(liquibase);
        }
    }

    private Database createDatabaseConnectionFromConfiguration(LiquibaseConfig configuration) {
        try {
            return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(getConnection(configuration)));
        } catch (DatabaseException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection(LiquibaseConfig conf) {
        try {
            DbDriver.loadDriver(conf.getUrl());
            Connection connection = DriverManager.getConnection(conf.getUrl(), conf.getUser(), conf.getPassword());
            if (conf.getSchema() != null) {
                connection.setSchema(conf.getSchema());
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run this migration.
     * @throws LiquibaseException on error
     */
    public void run() throws LiquibaseException {
        for (Liquibase liquibase : liquibaseList) {
            liquibase.update((String)null);
        }
    }

    /**
     * Drop all db objects.
     * @throws DatabaseException on error
     */
    public void drop() throws DatabaseException {
        for (Liquibase liquibase : liquibaseList) {
            liquibase.dropAll();
            break;
        }
        LockServiceFactory.getInstance().resetAll();
    }

    /**
     * Force drop all db objects.
     * @throws DatabaseException on error
     */
    public void forceDrop() throws DatabaseException {
        for (Liquibase liquibase : liquibaseList) {
            Database         database = liquibase.getDatabase();
            CatalogAndSchema schema   = new CatalogAndSchema(database.getDefaultCatalogName(), database.getDefaultSchemaName());
            try {
                liquibase.checkLiquibaseTables(false, null, new Contexts(), new LabelExpression());
                database.dropDatabaseObjects(schema);
            } catch (DatabaseException e) {
                throw e;
            } catch (Exception e) {
                throw new DatabaseException(e);
            } finally {
                LockServiceFactory.getInstance().getLockService(database).destroy();
                LockServiceFactory.getInstance().resetAll();
                ChangeLogHistoryServiceFactory.getInstance().resetAll();
                ExecutorService.getInstance().reset();
            }
            return;
        }
    }

    void exit() {
        System.exit(0);
    }

    class UrlAwareClassLoaderResourceAccessor extends ClassLoaderResourceAccessor {
        private String realFileName;

        public UrlAwareClassLoaderResourceAccessor(String realFileName) {
            this.realFileName = realFileName;
        }

        @Override
        public Set<InputStream> getResourcesAsStream(String path) throws IOException {
            if (realFileName.endsWith(path)) {
                path = realFileName;
            }

            if (path.startsWith("file:") || path.startsWith("jar:file:")) {
                URL         url              = new URL(path);
                InputStream resourceAsStream = url.openStream();

                return Sets.newHashSet(resourceAsStream);
            }

            return super.getResourcesAsStream(path);
        }
    }
}
