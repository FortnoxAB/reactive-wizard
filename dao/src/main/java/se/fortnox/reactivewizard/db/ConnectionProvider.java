package se.fortnox.reactivewizard.db;

import java.sql.Connection;

public interface ConnectionProvider {
    Connection get();
}
