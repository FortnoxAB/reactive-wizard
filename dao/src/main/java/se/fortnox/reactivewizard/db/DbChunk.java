package se.fortnox.reactivewizard.db;

import rx.Observable;
import se.fortnox.reactivewizard.CollectionOptions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static rx.Observable.defer;

public abstract class DbChunk {

    private static final int DEFAULT_CHUNK_SIZE = 500;

    /**
     * Provides a convenient way to select smaller chunks of data at the time from the database. Useful when we want to reduce the memory footprint while
     * fetching large amout of data from the database.
     * <p>
     * Combine with Flux as return type or use the @Stream annotation on the resource method to stream the result to the client.
     * <p>
     * Example:
     * <pre>
     *     Observable<User> users = inChunks(usersDao::listUsers);
     * </pre>
     * <p>
     * {@code chunkSize} defaults to 500.
     *
     * @param daoCall   A function that calls the desired dao with the supplied {@link CollectionOptions}. The collection options must be passed to the dao.
     * @param chunkSize size of the chunks to fetch
     * @return A observable that selects the data with the provided dao in chunks.
     */
    public static <T> Observable<T> inChunks(Function<CollectionOptions, Observable<T>> daoCall, int chunkSize) {
        AtomicInteger currentOffset = new AtomicInteger();
        return defer(() -> {
            CollectionOptions collectionOptions = new CollectionOptions();
            collectionOptions.setOffset(currentOffset.get());
            collectionOptions.setLimit(chunkSize);
            return daoCall.apply(collectionOptions).toList();
        })
            .doOnNext((ignore) -> currentOffset.addAndGet(chunkSize))
            .repeat()
            .takeUntil(result -> result.size() < chunkSize)
            .flatMapIterable(e -> e);
    }

    /**
     * @see DbChunk#inChunks(Function, int)
     */
    public static <T> Observable<T> inChunks(Function<CollectionOptions, Observable<T>> daoCall) {
        return inChunks(daoCall, DEFAULT_CHUNK_SIZE);
    }

    private DbChunk() {
    }
}
