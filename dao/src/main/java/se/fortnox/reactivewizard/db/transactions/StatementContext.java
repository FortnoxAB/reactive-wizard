package se.fortnox.reactivewizard.db.transactions;

import rx.Scheduler;
import se.fortnox.reactivewizard.db.ConnectionProvider;
import se.fortnox.reactivewizard.db.statement.Statement;

public record StatementContext(Statement statement, ConnectionProvider connectionProvider, Scheduler scheduler) { }
