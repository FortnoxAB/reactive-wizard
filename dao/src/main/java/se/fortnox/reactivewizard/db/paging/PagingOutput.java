package se.fortnox.reactivewizard.db.paging;

import rx.Observable;
import se.fortnox.reactivewizard.CollectionOptions;

import java.lang.reflect.Method;

import static com.google.common.collect.Iterables.indexOf;
import static java.util.Arrays.asList;

public class PagingOutput {
    private final int index;

    public PagingOutput(Method method) {
        index = indexOf(asList(method.getParameterTypes()), CollectionOptions.class::isAssignableFrom);
    }

    /**
     * Apply paging to result.
     * @param result the result
     * @param args the arguments
     * @param <T> the type of result
     * @return the paged result
     */
    public <T> Observable<T> apply(Observable<T> result, Object[] args) {
        if (index == -1) {
            return result;
        }

        CollectionOptions collectionOptions = (CollectionOptions)args[index];
        if (collectionOptions == null || collectionOptions.getLimit() == null) {
            return result;
        }

        return result.lift(new PagingOperator(collectionOptions));
    }
}
