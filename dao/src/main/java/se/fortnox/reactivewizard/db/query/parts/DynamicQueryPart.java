package se.fortnox.reactivewizard.db.query.parts;

import java.sql.SQLException;

public interface DynamicQueryPart extends QueryPart {
    DynamicQueryPart subPath(String[] subPath) throws SQLException;
}
