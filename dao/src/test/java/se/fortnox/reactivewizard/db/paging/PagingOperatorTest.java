package se.fortnox.reactivewizard.db.paging;

import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.CollectionOptions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PagingOperatorTest {

    private static final int PAGE_SIZE = 3;

    @Test
    public void shouldManipulateCollectionOptionsCorrectly() {
        shouldBeLastRecord(fetchPageFromStreamWithSize(0));
        shouldBeLastRecord(fetchPageFromStreamWithSize(PAGE_SIZE-1));
        shouldBeLastRecord(fetchPageFromStreamWithSize(PAGE_SIZE));
        shouldNotBeLastRecord(fetchPageFromStreamWithSize(PAGE_SIZE+1));
    }

    @Test
    public void shouldSignalLastRecordIfLimitIsNull() {
        CollectionOptions collectionOptions = new CollectionOptions();
        collectionOptions.setLimit(null);

        PagingOperator<Integer> pagingOperator = new PagingOperator<>(collectionOptions);

        assertThat(collectionOptions.isLastRecord()).isFalse();
        final List<Integer> items = Observable.range(1, 5).lift(pagingOperator).toList().toBlocking().first();
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }


    private void shouldBeLastRecord(CollectionOptions collectionOptions) {
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }

    private void shouldNotBeLastRecord(CollectionOptions collectionOptions) {
        assertThat(collectionOptions.isLastRecord()).isFalse();
    }

    private CollectionOptions fetchPageFromStreamWithSize(int streamSize) {
        CollectionOptions collectionOptions = new CollectionOptions();
        collectionOptions.setLimit(PAGE_SIZE);

        PagingOperator<Integer> pagingOperator = new PagingOperator<>(collectionOptions);

        final List<Integer> items = Observable.range(1, streamSize).lift(pagingOperator).toList().toBlocking().first();
        assertThat(items).hasSize(Math.min(PAGE_SIZE, streamSize));
        return collectionOptions;
    }

}
