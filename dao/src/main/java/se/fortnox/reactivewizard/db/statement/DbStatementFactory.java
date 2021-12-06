package se.fortnox.reactivewizard.db.statement;

public interface DbStatementFactory {

    Statement create(Object[] args);
}
