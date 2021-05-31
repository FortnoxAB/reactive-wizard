package se.fortnox.reactivewizard.db;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import se.fortnox.reactivewizard.CollectionOptions;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;
import static rx.Observable.error;
import static rx.Observable.just;
import static se.fortnox.reactivewizard.test.observable.ObservableAssertions.*;

@RunWith(MockitoJUnitRunner.class)
public class DbChunkTest {

    @Mock
    MockDao dao;

    @Before
    public void setup() {
        reset(dao);
    }

    @Test
    public void shouldFetchTheRowsInChunksOfSpecifiedSize() {
        int chunkSize = 2;

        when(dao.selectQuery(any()))
            .thenReturn(just("a","b"))
            .thenReturn(just("c","d"))
            .thenReturn(just("e"))
            .thenReturn(just("f","g"));

        List<String> rows = DbChunk.inChunks(collectionOptions -> dao.selectQuery(collectionOptions), chunkSize)
            .toList()
            .toBlocking()
            .singleOrDefault(null);

        assertThat(rows.size())
            .isEqualTo(5);

        verify(dao, times(3))
            .selectQuery(any(CollectionOptions.class));
    }

    @Test
    public void shouldNotContinueToFetchFromDatabaseIfNoDataAtAll() {
        int chunkSize = 2;

        when(dao.selectQuery(any()))
            .thenReturn(empty());

        List<String> rows = DbChunk.inChunks(collectionOptions -> dao.selectQuery(collectionOptions), chunkSize)
            .toList()
            .toBlocking()
            .singleOrDefault(null);

        assertThat(rows.isEmpty())
            .isTrue();

        verify(dao, times(1))
            .selectQuery(any(CollectionOptions.class));
    }

    @Test
    public void shouldNotContinueIfExceptionOccurs() {
        int chunkSize = 2;

        when(dao.selectQuery(any()))
            .thenReturn(just("a","b"))
            .thenReturn(error(new SQLException("expected exception")))
            .thenReturn(just("c","d"));

        assertThatExceptionOfType(RuntimeException.class)
            .isThrownBy( () -> DbChunk.inChunks(collectionOptions -> dao.selectQuery(collectionOptions), chunkSize).toBlocking().last());

        verify(dao, times(2))
            .selectQuery(any(CollectionOptions.class));
    }


    @Test
    public void shouldDefaultToChunksOfSize500IfNotSpecified() {
        when(dao.selectQuery(any()))
            .thenReturn(Observable.range(1,500).map(String::valueOf))
            .thenReturn(just("c","d"))
            .thenReturn(just("e","f"))
            .thenReturn(just("e","f"));

        List<String> rows = DbChunk.inChunks(dao::selectQuery)
            .toList()
            .toBlocking()
            .singleOrDefault(null);

        assertThat(rows.size())
            .isEqualTo(502);

        verify(dao, times(2))
            .selectQuery(any(CollectionOptions.class));
    }

    interface MockDao {
        public Observable<String> selectQuery(CollectionOptions collectionOptions);
    }
}
