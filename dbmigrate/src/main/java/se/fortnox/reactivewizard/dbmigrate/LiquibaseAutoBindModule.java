package se.fortnox.reactivewizard.dbmigrate;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.fortnox.reactivewizard.binding.AutoBindModule;
import se.fortnox.reactivewizard.config.ConfigFactory;
import com.google.inject.Binder;

/**
 * Runs liquibase before the system is started, if parameter "db-migrate" is used. The following alternatives exists:
 * <ul>
 *  <li> "db-migrate": Run migrations and shutdown system. Use this locally to avoid liquibase migrations during normal run.</li>
 *  <li> "db-migrate-run": Run migrations and start the system. Use this in production, when you want to run migrations before the system is started.</li>
 *  <li> "db-drop-migrate": Drop database, run migrations and shutdown system. Use this locally to get a clean start.</li>
 *  <li> "db-drop-migrate-run": Drop database, run migrations and start the system. Use this locally to get a clean start and a running system.</li>
 * </ul>
 */
public class LiquibaseAutoBindModule implements AutoBindModule {

    private static Logger LOG = LoggerFactory.getLogger(LiquibaseAutoBindModule.class);
    private final LiquibaseConfig liquibaseConfig;
    private final String startCommand;

    @Inject
    public LiquibaseAutoBindModule(ConfigFactory confFactory, @Named("args") String[] args) {
        liquibaseConfig = confFactory.get(LiquibaseConfig.class);
        if (args == null || args.length < 2) {
            this.startCommand = "";
        } else {
            this.startCommand = args[0];
        }
    }

    @Override
    public void preBind() {
        if (startCommand.startsWith("db-")) {
            try {
                LiquibaseMigrate migrator = new LiquibaseMigrate(liquibaseConfig);
                if (startCommand.startsWith("db-drop")) {
                    try {
                        migrator.drop();
                    } catch (Exception e) {
                        LOG.warn("Can't drop db. Will proceed.", e);
                    }
                }

                if (startCommand.contains("migrate")) {
                    migrator.run();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (!startCommand.endsWith("-run")) {
                System.exit(0);
            }
        }
    }

    @Override
    public void configure(Binder binder) {

    }
}
