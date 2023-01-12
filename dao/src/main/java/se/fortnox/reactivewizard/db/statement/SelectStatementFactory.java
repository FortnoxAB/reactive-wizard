package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.FluxSink;
import se.fortnox.reactivewizard.db.deserializing.DbResultSetDeserializer;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SelectStatementFactory extends AbstractDbStatementFactory {
    private final DbResultSetDeserializer deserializer;

    public SelectStatementFactory(ParameterizedQuery parameterizedQuery, Class<?> returnType) {
        super(parameterizedQuery);
        this.deserializer = new DbResultSetDeserializer(returnType);
    }

    @Override
    protected void executeStatement(Connection connection, Object[] args, FluxSink fluxSink)
        throws SQLException {
        try (PreparedStatement statement = parameterizedQuery.createStatement(connection, args)) {
            parameterizedQuery.addParameters(args, statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    if (fluxSink != null) {
                        var value = deserializer.deserialize(resultSet);
                        if (value == null) {
                            var sqlQuery = parameterizedQuery.toString();
                            throw new NullPointerException("""
                                One or more of the values returned in the resultset of the following query was null:
                                {{sqlQuery}}
                                  
                                Project Reactor does not allow emitting null values in a stream. Wrap the return value from the dao interface
                                in a 'wrapper' to solve the issue.
                                Example: 
                                record Wrapper(String nullableValue) {};
                                """.replace("{{sqlQuery}}", sqlQuery));
                        }
                        fluxSink.next(value);
                    }
                }
            }
            StatementDebug.log(statement);
        }
    }

    @Override
    public String toString() {
        return parameterizedQuery.toString();
    }
}
