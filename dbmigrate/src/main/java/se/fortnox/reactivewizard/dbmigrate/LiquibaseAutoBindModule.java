package se.fortnox.reactivewizard.dbmigrate;

import com.google.inject.Binder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fortnox.reactivewizard.binding.AutoBindModule;

/**
 * Runs liquibase before the system is started, if parameter "db-migrate" is used. The following alternatives exist:
 * <ul>
 * <li> "db-migrate": Run migrations and shutdown system. Use this locally to avoid liquibase migrations during normal run.</li>
 * <li> "db-migrate-run": Run migrations and start the system. Use this in production, when you want to run migrations before the system is started.</li>
 * <li> "db-drop-migrate": Drop database, run migrations and shutdown system. Use this locally to get a clean start.</li>
 * <li> "db-drop-migrate-run": Drop database, run migrations and start the system. Use this locally to get a clean start and a running system.</li>
 * </ul>
 */
public class LiquibaseAutoBindModule implements AutoBindModule {
    private static final Logger                     LOG = LoggerFactory.getLogger(LiquibaseAutoBindModule.class);
    private final        String                     startCommand;
    private final        LiquibaseMigrateProvider liquibaseMigrateProvider;

    @Inject
    public LiquibaseAutoBindModule(@Named("args") String[] args, LiquibaseMigrateProvider liquibaseMigrateProvider) {
        this.liquibaseMigrateProvider = liquibaseMigrateProvider;
        if (args.length < 2) {
            this.startCommand = "";
        } else {
            this.startCommand = args[0];
        }
    }

    @Override
    public void preBind() {
        if (!startCommand.startsWith("db-")) {
            return;
        }

        LiquibaseMigrate liquibaseMigrate = liquibaseMigrateProvider.get();
        try {
            if (startCommand.startsWith("db-drop")) {
                drop(liquibaseMigrate);
            } else if (startCommand.startsWith("db-rollback")) {
                rollback(liquibaseMigrate);
            }

            if (startCommand.contains("migrate")) {
                liquibaseMigrate.run();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!startCommand.endsWith("-run")) {
            liquibaseMigrate.exit();
        }
    }

    private static void drop(LiquibaseMigrate liquibaseMigrate) {
        try {
            liquibaseMigrate.drop();
        } catch (Exception e) {
            LOG.warn("Can't drop db. Will proceed.", e);
        }
    }

    private static void rollback(LiquibaseMigrate liquibaseMigrate) {
        try {
            liquibaseMigrate.rollback();
        } catch (Exception e) {
            LOG.warn("Can't rollback migration. Will proceed.", e);
        }
    }

    @Override
    public void configure(Binder binder) {

    }
}
