package se.fortnox.reactivewizard.db.paging;

import rx.Observable;
import se.fortnox.reactivewizard.CollectionOptions;

import java.lang.reflect.Method;

import static com.google.common.collect.Iterables.indexOf;
import static java.util.Arrays.asList;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class PagingOutput {
    private final int index;

    public PagingOutput(Method method) {
        index = indexOf(asList(method.getParameterTypes()), CollectionOptions.class::isAssignableFrom);
    }

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
