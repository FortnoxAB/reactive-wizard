package se.fortnox.reactivewizard.dbmigrate;

import liquibase.exception.LiquibaseException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class LiquibaseMigrateTest {

    private LiquibaseConfig  liquibaseConfig;
    private LiquibaseMigrate liquibaseMigrate;

    @Before
    public void setup() throws IOException, LiquibaseException {
        liquibaseConfig = new LiquibaseConfig();
        liquibaseConfig.setUrl("jdbc:h2:mem:testliquibase;INIT=CREATE SCHEMA IF NOT EXISTS TESTSCHEMA AUTHORIZATION SA");
        liquibaseConfig.setUser("sa");
        liquibaseConfig.setSchema("TESTSCHEMA");
        liquibaseConfig.setMigrationsFile("migrations.xml");
        liquibaseMigrate = new LiquibaseMigrate(liquibaseConfig);
    }

    @Test
    public void shouldMigrate() throws LiquibaseException, SQLException {
        liquibaseMigrate.run();
        Connection connection = getConnection(liquibaseConfig);
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("select * from TESTSCHEMA.test");
            resultSet.next();
            assertThat(resultSet.getInt(1)).isEqualTo(4);
        } finally {
            connection.close();
        }

    }

    @Test
    public void shouldForceDropAllTables() throws LiquibaseException, SQLException {
        liquibaseMigrate.run();
        simulateLock(liquibaseConfig);

        liquibaseMigrate.forceDrop();
        liquibaseMigrate.run();

        ensureNotLocked(liquibaseConfig);
    }

    @Test
    public void shouldDropAllTables() throws LiquibaseException {
        liquibaseMigrate.run();
        liquibaseMigrate.drop();
        liquibaseMigrate.run();
    }

    @Test
    public void shouldThrowExceptionWhenMissingMigrationsFile() throws IOException, LiquibaseException {
        LiquibaseConfig localConfig = liquibaseConfig;
        localConfig.setMigrationsFile("notfound.xml");
        try {
            new LiquibaseMigrate(localConfig);
            fail("Expected RuntimeException, but none was thrown");
        } catch (RuntimeException expectedException) {
            assertThat(expectedException.getMessage()).isEqualTo("Could not find migrations file notfound.xml");
        }
    }

    private void ensureNotLocked(LiquibaseConfig liquibaseConfig) throws SQLException {
        Connection connection = getConnection(liquibaseConfig);
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("SELECT locked FROM TESTSCHEMA.databasechangeloglock");
            resultSet.next();
            assertThat(resultSet.getBoolean(1)).isFalse();
        } finally {
            connection.close();
        }

    }

    private void simulateLock(LiquibaseConfig liquibaseConfig) throws SQLException {
        Connection connection = getConnection(liquibaseConfig);
        try {
            connection.createStatement().executeUpdate("UPDATE TESTSCHEMA.databasechangeloglock SET locked=true");
        } finally {
            connection.close();
        }
    }

    private Connection getConnection(LiquibaseConfig liquibaseConfig) throws SQLException {
        return DriverManager.getConnection(liquibaseConfig.getUrl(), liquibaseConfig.getUser(), liquibaseConfig.getPassword());
    }
}
