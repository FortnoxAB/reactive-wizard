package se.fortnox.reactivewizard.db.paging;

import reactor.core.publisher.Flux;
import se.fortnox.reactivewizard.CollectionOptions;
import se.fortnox.reactivewizard.db.Query;

import java.lang.reflect.Method;

import static com.google.common.collect.Iterables.indexOf;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static reactor.core.publisher.Operators.liftPublisher;

public class PagingOutput {
    private final int index;
    private final int defaultLimit;
    private final int maxLimit;

    public PagingOutput(Method method) {
        index = indexOf(asList(method.getParameterTypes()), CollectionOptions.class::isAssignableFrom);

        if (method.isAnnotationPresent(Query.class)) {
            final Query queryAnnotation = method.getAnnotation(Query.class);
            defaultLimit = queryAnnotation.defaultLimit();
            maxLimit = queryAnnotation.maxLimit();
        } else {
            defaultLimit = 100;
            maxLimit = 1000;
        }
    }

    /**
     * Apply paging to result.
     *
     * @param result the result
     * @param args   the arguments
     * @param <T>    the type of result
     * @return the paged result
     */
    public <T> Flux<T> apply(Flux<T> result, Object[] args) {
        if (index == -1) {
            return result;
        }

        CollectionOptions collectionOptions = (CollectionOptions) args[index];
        if (collectionOptions == null) {
            return result;
        }

        if (collectionOptions.getLimit() == null || collectionOptions.getLimit() < 0) {
            collectionOptions.setLimit(defaultLimit);
        }

        collectionOptions.setLimit(min(maxLimit, collectionOptions.getLimit()));
        return result.transformDeferred(liftPublisher(new PagingOperator(collectionOptions)));
    }
}
