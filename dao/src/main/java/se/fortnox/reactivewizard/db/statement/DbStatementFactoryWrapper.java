package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.query.ParameterizedQuery;

public class DbStatementFactoryWrapper implements DbStatementFactory {
    private final ParameterizedQuery parameterizedQuery;
    private final DbStatementFactory statementFactory;

    public DbStatementFactoryWrapper(ParameterizedQuery parameterizedQuery, DbStatementFactory statementFactory) {
        this.parameterizedQuery = parameterizedQuery;
        this.statementFactory = statementFactory;
    }

    @Override
    public Statement create(Object[] args) {
        return statementFactory.create(args);
    }

    @Override
    public String toString() {
        return parameterizedQuery.toString();
    }
}
