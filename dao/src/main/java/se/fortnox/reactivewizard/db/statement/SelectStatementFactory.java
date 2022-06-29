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
                        fluxSink.next(deserializer.deserialize(resultSet));
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
