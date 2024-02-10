package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.GeneratedKey;
import se.fortnox.reactivewizard.db.deserializing.DbResultSetDeserializer;
import se.fortnox.reactivewizard.db.query.ParamSetter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UpdateReturningGeneratedKeySqlStatement<T> extends UpdateSqlStatement<GeneratedKey<T>> {

    private final DbResultSetDeserializer<T> deserializer;

    public UpdateReturningGeneratedKeySqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected, DbResultSetDeserializer<T> deserializer) {
        this(sql, paramSetters, minimumAffected, sql, deserializer);
    }

    public UpdateReturningGeneratedKeySqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected, String rawSql, DbResultSetDeserializer<T> deserializer) {
        super(sql, paramSetters, minimumAffected, rawSql);
        this.deserializer = deserializer;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (var preparedStatement = createPreparedStatementWithGeneratedKeys(connection)) {
            addParameters(preparedStatement);
            ensureMinimumReached(preparedStatement.executeUpdate());
            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (fluxSink != null) {
                    while (resultSet.next()) {
                        fluxSink.next(() -> deserializer.deserialize(resultSet));
                    }
                } else if (monoSink != null) {
                    if (resultSet.next()) {
                        monoSink.success(() -> deserializer.deserialize(resultSet));
                    } else {
                        monoSink.success();
                    }
                }
            }
            StatementDebug.log(preparedStatement);
        }
    }

    @Override
    public PreparedStatement batch(Connection connection, PreparedStatement preparedStatement) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void batchExecuted(int count) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean sameBatch(Statement statement) {
        return false;
    }
}
