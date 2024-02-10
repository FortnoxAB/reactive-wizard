package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.deserializing.DbResultSetDeserializer;
import se.fortnox.reactivewizard.db.query.ParamSetter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static java.lang.String.format;

public class SelectSqlStatement<T> extends SqlStatement<T> {

    private static final String NULL_VALUE_ERROR = """
        One or more of the values returned in the resultset of the following query was null:
        %s
          
        Project Reactor does not allow emitting null values in a stream. Wrap the return value from the dao interface
        in a 'wrapper' to solve the issue.
        Example:
        record Wrapper(String nullableValue) {};
        """;

    private static final String MONO_NEXT_ERROR = "%s returning a Mono received more than one result from the database";

    private final DbResultSetDeserializer<T> deserializer;

    private final String methodName;
    private final String rawSql;

    public SelectSqlStatement(String sql, List<ParamSetter> paramSetters,
        DbResultSetDeserializer<T> deserializer) {
        this(sql, paramSetters, deserializer, "Query\n" + sql + "\n", sql);
    }

    public SelectSqlStatement(String sql, List<ParamSetter> paramSetters,
        DbResultSetDeserializer<T> deserializer, String methodName, String rawSql) {
        super(sql, paramSetters);
        this.deserializer = deserializer;
        this.methodName = methodName;
        this.rawSql = rawSql;
    }

    @Override
    public void execute(Connection connection) throws SQLException {
        try (var preparedStatement = createPreparedStatement(connection)) {
            addParameters(preparedStatement);
            try (var resultSet = preparedStatement.executeQuery()) {
                if (fluxSink != null) {
                    while (resultSet.next()) {
                        fluxSink.next(deserialize(resultSet));
                    }
                } else if (monoSink != null) {
                    if (resultSet.next()) {
                        var object = deserialize(resultSet);
                        if (resultSet.next()) {
                            throw new RuntimeException(format(MONO_NEXT_ERROR, methodName));
                        }
                        monoSink.success(object);
                    } else {
                        monoSink.success();
                    }
                }
            }
            StatementDebug.log(preparedStatement);
        }
    }

    private T deserialize(ResultSet resultSet) {
        var value = deserializer.deserialize(resultSet);
        if (value == null) {
            throw new NullPointerException(format(NULL_VALUE_ERROR, rawSql));
        }
        return value;
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
