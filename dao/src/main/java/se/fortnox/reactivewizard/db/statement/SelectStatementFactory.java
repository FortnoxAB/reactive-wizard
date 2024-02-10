package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;
import se.fortnox.reactivewizard.db.deserializing.DbResultSetDeserializerImpl;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static java.lang.String.format;

public class SelectStatementFactory extends AbstractDbStatementFactory {
    private static final String NULL_VALUE_ERROR = """
        One or more of the values returned in the resultset of the following query was null:
        {{sqlQuery}}
          
        Project Reactor does not allow emitting null values in a stream. Wrap the return value from the dao interface
        in a 'wrapper' to solve the issue.
        Example: 
        record Wrapper(String nullableValue) {};
        """;
    private static final String QUERY_PLACEHOLDER = "{{sqlQuery}}";
    private static final String MONO_NEXT_ERROR = "%s returning a Mono received more than one result from the database";
    private final DbResultSetDeserializerImpl deserializer;

    private final String methodName;

    public SelectStatementFactory(ParameterizedQuery parameterizedQuery, Class<?> returnType) {
        super(parameterizedQuery);
        this.deserializer = new DbResultSetDeserializerImpl(returnType);
        this.methodName = parameterizedQuery.getMethodName();
    }

    @Override
    protected void executeStatement(Connection connection, Object[] args, FluxSink fluxSink) throws SQLException {
        try (PreparedStatement statement = parameterizedQuery.createStatement(connection, args)) {
            parameterizedQuery.addParameters(args, statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (fluxSink != null) {
                        fluxSink.next(deserialize(resultSet));
                    }
                }
            }
        }
    }

    @Override
    protected void executeStatement(Connection connection, Object[] args, MonoSink monoSink) throws SQLException {
        try (PreparedStatement statement = parameterizedQuery.createStatement(connection, args)) {
            parameterizedQuery.addParameters(args, statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (monoSink == null) {
                    return;
                }

                Object object = null;

                if (resultSet.next()) {
                    object = deserialize(resultSet);
                } else {
                    monoSink.success();
                }

                if (resultSet.next()) {
                    throw new RuntimeException(format(MONO_NEXT_ERROR, methodName));
                }

                monoSink.success(object);
            }
            StatementDebug.log(statement);
        }
    }

    @Override
    public String toString() {
        return parameterizedQuery.toString();
    }

    private Object deserialize(ResultSet resultSet) {
        var value = deserializer.deserialize(resultSet);
        if (value == null) {
            var sqlQuery = parameterizedQuery.toString();
            throw new NullPointerException(NULL_VALUE_ERROR.replace(QUERY_PLACEHOLDER, sqlQuery));
        }
        return value;
    }
}
