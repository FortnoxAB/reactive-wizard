package se.fortnox.reactivewizard.db.query;

import java.sql.SQLException;

public interface ParamSetter {
    void call(PreparedStatementParameters parameters) throws SQLException;
}
