package se.fortnox.reactivewizard.dbmigrate;

import com.google.inject.Inject;
import liquibase.exception.LiquibaseException;
import se.fortnox.reactivewizard.config.ConfigFactory;

import java.io.IOException;

public class LiquibaseMigrateProvider {
    private final LiquibaseMigrate liquibaseMigrate;

    @Inject
    public LiquibaseMigrateProvider(ConfigFactory configFactory) throws IOException, LiquibaseException {
        LiquibaseConfig liquibaseConfig = configFactory.get(LiquibaseConfig.class);
        liquibaseMigrate = new LiquibaseMigrate(liquibaseConfig);
    }

    public LiquibaseMigrate get() {
        return liquibaseMigrate;
    }
}
