package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.query.ParamSetter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public abstract class UpdateBatchableSqlStatement<T> extends UpdateSqlStatement<T> {

    protected UpdateBatchableSqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected, String rawSql) {
        super(sql, paramSetters, minimumAffected, rawSql);
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (var preparedStatement = createPreparedStatement(connection)) {
            addParameters(preparedStatement);
            int count = preparedStatement.executeUpdate();
            ensureMinimumReached(count);
            executed(count);
            StatementDebug.log(preparedStatement);
        }
    }

    protected abstract void executed(int count);

    @Override
    public PreparedStatement batch(Connection connection, PreparedStatement preparedStatement) throws SQLException {
        if (preparedStatement == null) {
            preparedStatement = createPreparedStatement(connection);
        }
        addParameters(preparedStatement);
        preparedStatement.addBatch();
        return preparedStatement;
    }

    @Override
    public void batchExecuted(int count) throws SQLException {
        ensureMinimumReached(count);
    }

    @Override
    public boolean sameBatch(Statement statement) {
        if (!(statement instanceof UpdateBatchableSqlStatement<?> batchableStatement)) {
            return false;
        }
        return batchableStatement.sql.equals(this.sql);
    }
}
