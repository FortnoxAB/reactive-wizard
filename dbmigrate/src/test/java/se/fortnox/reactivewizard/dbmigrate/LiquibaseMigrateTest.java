package se.fortnox.reactivewizard.dbmigrate;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import liquibase.exception.LiquibaseException;
import org.junit.Before;
import org.junit.Test;

public class LiquibaseMigrateTest {

    private LiquibaseConfig conf;
    private LiquibaseMigrate liquibaseMigrate;

    @Before
    public void setup() throws IOException, LiquibaseException, SQLException {
        conf = new LiquibaseConfig();
        conf.setUrl("jdbc:h2:mem:testliquibase;INIT=CREATE SCHEMA IF NOT EXISTS TESTSCHEMA AUTHORIZATION SA");
        conf.setUser("sa");
        conf.setSchema("TESTSCHEMA");
        liquibaseMigrate = new LiquibaseMigrate(conf);
    }

    @Test
    public void test() throws LiquibaseException, SQLException {
        liquibaseMigrate.run();
        Connection connection = getConnection(conf);
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("select * from TESTSCHEMA.test");
            resultSet.next();
            assertThat(resultSet.getInt(1)).isEqualTo(4);
        } finally {
            connection.close();
        }

    }

    @Test
    public void shouldForceDropAllTables() throws IOException, LiquibaseException, SQLException {
        liquibaseMigrate.run();
        simulateLock(conf);

        liquibaseMigrate.forceDrop();
        liquibaseMigrate.run();

        ensureNotLocked(conf);
    }

    private void ensureNotLocked(LiquibaseConfig conf) throws SQLException {
        Connection connection = getConnection(conf);
        try {
            ResultSet resultSet = connection.createStatement().executeQuery("select locked from TESTSCHEMA.databasechangeloglock");
            resultSet.next();
            assertThat(resultSet.getBoolean(1)).isFalse();
        } finally {
            connection.close();
        }

    }

    private void simulateLock(LiquibaseConfig conf) throws SQLException {
        Connection connection = getConnection(conf);
        try {
            connection.createStatement().executeUpdate("update TESTSCHEMA.databasechangeloglock set locked=true");
        } finally {
            connection.close();
        }
    }

    private Connection getConnection(LiquibaseConfig conf) throws SQLException {
        return DriverManager.getConnection(conf.getUrl(), conf.getUser(), conf.getPassword());
    }
}
