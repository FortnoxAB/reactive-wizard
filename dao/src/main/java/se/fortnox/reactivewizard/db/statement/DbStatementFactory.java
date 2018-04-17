package se.fortnox.reactivewizard.db.statement;

import rx.Subscriber;

public interface DbStatementFactory {

    Statement create(Object[] args, Subscriber subscriber);
}
