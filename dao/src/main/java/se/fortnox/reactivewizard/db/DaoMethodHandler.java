package se.fortnox.reactivewizard.db;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.fortnox.reactivewizard.db.paging.PagingOutput;
import se.fortnox.reactivewizard.db.statement.DbStatementFactory;
import se.fortnox.reactivewizard.metrics.Metrics;

import java.lang.reflect.Method;

public class DaoMethodHandler {

    private final Method method;
    private final DbStatementFactory statementFactory;
    private final PagingOutput pagingOutput;
    private final Metrics metrics;

    public DaoMethodHandler(Method method,
        DbStatementFactory statementFactory, PagingOutput pagingOutput, Metrics metrics) {
        this.method = method;
        this.statementFactory = statementFactory;
        this.pagingOutput = pagingOutput;
        this.metrics = metrics;
    }

    public Publisher<Object> run(Object[] args, ReactiveStatementFactory reactiveStatementFactory) {
        if (Mono.class.isAssignableFrom(method.getReturnType())) {
            return reactiveStatementFactory.createMono(
                metrics,
                () -> statementFactory.create(args)
            );
        } else if (Flux.class.isAssignableFrom(method.getReturnType())) {
            return reactiveStatementFactory.createFlux(
                metrics,
                () -> statementFactory.create(args),
                flux -> pagingOutput.apply(flux, args)
            );
        } else {
            throw new IllegalArgumentException(String.format(
                "DAO method %s::%s must return a Flux or Mono. Found %s",
                method.getDeclaringClass().getName(),
                method.getName(),
                method.getReturnType().getName()
            ));
        }
    }
}
