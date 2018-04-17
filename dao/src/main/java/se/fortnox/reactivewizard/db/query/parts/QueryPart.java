package se.fortnox.reactivewizard.db.query.parts;

import se.fortnox.reactivewizard.db.query.PreparedStatementParameters;

import java.sql.SQLException;

public interface QueryPart {

    void visit(StringBuilder sql, Object[] args);

    void addParams(PreparedStatementParameters preparedStatement, Object[] args) throws SQLException;
}
