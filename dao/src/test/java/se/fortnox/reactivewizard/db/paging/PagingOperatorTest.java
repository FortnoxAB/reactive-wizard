package se.fortnox.reactivewizard.db.paging;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import se.fortnox.reactivewizard.CollectionOptions;

import static java.lang.Math.min;
import static org.assertj.core.api.Assertions.assertThat;
import static reactor.core.publisher.Flux.range;
import static reactor.core.publisher.Operators.liftPublisher;

public class PagingOperatorTest {

    private static final int PAGE_SIZE = 3;

    @Test
    public void shouldManipulateCollectionOptionsCorrectly() {
        shouldBeLastRecord(fetchPageFromStreamWithSize(0));
        shouldBeLastRecord(fetchPageFromStreamWithSize(PAGE_SIZE - 1));
        shouldBeLastRecord(fetchPageFromStreamWithSize(PAGE_SIZE));
        shouldNotBeLastRecord(fetchPageFromStreamWithSize(PAGE_SIZE + 1));
    }

    @Test
    public void shouldSignalLastRecordIfLimitIsNull() {
        CollectionOptions collectionOptions = new CollectionOptions();
        collectionOptions.setLimit(null);

        PagingOperator pagingOperator = new PagingOperator<>(collectionOptions);

        assertThat(collectionOptions.isLastRecord()).isFalse();
        StepVerifier.create(range(1, 5).transformDeferred(liftPublisher(pagingOperator)))
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();
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

        PagingOperator pagingOperator = new PagingOperator<>(collectionOptions);

        Flux<Integer> items = range(1, streamSize).transformDeferred(liftPublisher(pagingOperator));
        assertThat(items.collectList().block())
                .hasSize(min(PAGE_SIZE, streamSize));
        return collectionOptions;
    }

}
