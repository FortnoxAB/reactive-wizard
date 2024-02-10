package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.query.ParamSetter;

import java.sql.SQLException;
import java.util.List;

public abstract class UpdateSqlStatement<T> extends SqlStatement<T> {

    private final int minimumAffected;
    private final String rawSql;

    protected UpdateSqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected, String rawSql) {
        super(sql, paramSetters);
        this.minimumAffected = minimumAffected;
        this.rawSql = rawSql;
    }

    protected void ensureMinimumReached(int updateCount) throws SQLException {
        if (updateCount < minimumAffected) {
            throw new MinimumAffectedRowsException(minimumAffected, updateCount, rawSql);
        }
    }
}
