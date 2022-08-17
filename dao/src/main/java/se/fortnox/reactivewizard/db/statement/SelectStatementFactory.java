package se.fortnox.reactivewizard.db.statement;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import se.fortnox.reactivewizard.db.deserializing.DbResultSetDeserializer;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

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
                            throw new NullPointerException("""
                                One or more selected values from the database is null.
                                Project Reactor does not allow emitting null values in a stream. Wrap the return value from the dao interface
                                in a 'wrapping class' to solve the issue.
                                Example: 
                                class WrappingClass {    
                                    String maybeNullValue
                                }
                                """);
                        }
                        fluxSink.next(value);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return parameterizedQuery.toString();
    }
}
