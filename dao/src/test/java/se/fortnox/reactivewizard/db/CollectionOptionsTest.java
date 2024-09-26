package se.fortnox.reactivewizard.db;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import se.fortnox.reactivewizard.CollectionOptions;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 */
class CollectionOptionsTest {
    MockDb               mockDb               = new MockDb();
    DbProxy              proxy                = new DbProxy(new DatabaseConfig(), mockDb.getConnectionProvider());
    CollectionOptionsDao collectionOptionsDao = proxy.create(CollectionOptionsDao.class);

    @Test
    void shouldAddLimitFromCollectionOptions() throws SQLException {
        mockDb.addRows(4);
        final CollectionOptions collectionOptions = new CollectionOptions(3, null);
        assertThat(collectionOptionsDao.selectWithPaging(collectionOptions).collectList().block()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4");
        assertThat(collectionOptions.isLastRecord()).isFalse();
    }

    @Test
    void shouldAddOffsetFromCollectionOptions() throws SQLException {
        mockDb.addRows(3);
        final CollectionOptions collectionOptions = new CollectionOptions(null, 3);
        assertThat(collectionOptionsDao.selectWithPaging(collectionOptions).collectList().block()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 101 OFFSET 3");
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }

    @Test
    void shouldAddLimitAndOffsetFromCollectionOptions() throws SQLException {
        mockDb.addRows(4);
        final CollectionOptions collectionOptions = new CollectionOptions(3, 3);
        assertThat(collectionOptionsDao.selectWithPaging(collectionOptions).collectList().block()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 3");
        assertThat(collectionOptions.isLastRecord()).isFalse();
    }

    @Test
    void shouldNotSetOffsetIfNegative() throws SQLException {
        final CollectionOptions collectionOptions = new CollectionOptions(2, -1);
        collectionOptionsDao.selectWithPaging(collectionOptions).collectList().block();
        mockDb.verifySelect("select * from table LIMIT 3");
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }

    @Test
    void shouldNotSetLimitIfNegative() throws SQLException {
        final CollectionOptions collectionOptions = new CollectionOptions(-1, 2);
        collectionOptionsDao.selectWithPaging(collectionOptions).collectList().block();
        mockDb.verifySelect("select * from table LIMIT 101 OFFSET 2");
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }

    @Test
    void shouldNotSetLimitAboveMaxLimit() throws SQLException {
        final CollectionOptions collectionOptions = new CollectionOptions(5, 2);
        collectionOptionsDao.selectWithMaxLimit3(collectionOptions).collectList().block();
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 2");
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }

    @Test
    void shouldUseConfiguredDefaultLimit() throws SQLException {
        mockDb.addRows(10);
        final CollectionOptions collectionOptions = new CollectionOptions();
        StepVerifier.create(collectionOptionsDao.selectWithDefaultLimit10(collectionOptions))
            .expectNextCount(10)
            .verifyComplete();
        mockDb.verifySelect("select * from table LIMIT 11");
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }

    @Test
    void shouldUseConfiguredDefaultLimitOfOneWithReturnTypeMono() throws SQLException {
        mockDb.addRows(1);
        final CollectionOptions collectionOptions = new CollectionOptions();
        StepVerifier.create(collectionOptionsDao.selectWithDefaultLimit1(collectionOptions))
            .expectNextCount(1)
            .verifyComplete();
        mockDb.verifySelect("select * from table LIMIT 1");
        assertThat(collectionOptions.isLastRecord()).isFalse();
    }

    @Test
    void shouldSetLastRecordToFalseIfThereAreMoreRows() throws SQLException {
        mockDb.addRows(4);
        CollectionOptions collectionOptions = new CollectionOptions(3, 3);
        assertThat(collectionOptionsDao.selectWithPaging(collectionOptions).collectList().block()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 3");
        assertThat(collectionOptions.isLastRecord()).isFalse();
    }

    @Test
    void shouldSetLastRecordToTrueIfThereAreNoMoreRows() throws SQLException {
        mockDb.addRows(3);
        CollectionOptions collectionOptions = new CollectionOptions(3, 3);
        assertThat(collectionOptionsDao.selectWithPaging(collectionOptions).collectList().block()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 3");
        assertThat(collectionOptions.isLastRecord()).isTrue();
    }

    @Test
    void shouldNotAddCollectionOptionsAsParameter() throws SQLException {
        mockDb.addRows(4);
        assertThat(collectionOptionsDao.selectWithPaging(new CollectionOptions(3, null)).collectList().block()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4");
        verify(mockDb.getPreparedStatement(), never()).setObject(anyInt(), any());
    }

    @Test
    void shouldAddOrderByAsc() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithSorting(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table ORDER BY name ASC LIMIT 101");
    }

    @Test
    void shouldAddOrderByDesc() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.DESC);
        collectionOptionsDao.selectWithSorting(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table ORDER BY name DESC LIMIT 101");
    }

    @Test
    void shouldNotAddOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("badcolumn", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithSorting(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table LIMIT 101");
    }

    @Test
    void shouldAddOrderByAndLimit() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions(1, null, "name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithSorting(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table ORDER BY name ASC LIMIT 2");
    }

    @Test
    void shouldConvertCamelCaseToSnakeCase() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("userName", CollectionOptions.SortOrder.DESC);
        collectionOptionsDao.selectWithSortingCamelCase(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table ORDER BY user_name DESC LIMIT 101");
    }

    @Test
    void shouldAddOptionsOrderByBeforeQueryOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithDefaultSorting(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table order by name ASC, id LIMIT 101");
    }

    @Test
    void shouldAddDefaultSortBeforeQueryOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions();
        collectionOptionsDao.selectWithDefaultSortingInQueryAndOptions(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table order by text desc, id LIMIT 101");
    }

    @Test
    void shouldAddMultipleSort() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name asc, id desc", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithDefaultSortingAllowMultiple(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table order by name ASC, id DESC, id LIMIT 101");
    }

    @Test
    void shouldFallbackToCollectionOptionSortOrderMultipleSort() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name, id", CollectionOptions.SortOrder.DESC);
        collectionOptionsDao.selectWithDefaultSortingAllowMultiple(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table order by name DESC, id DESC, id LIMIT 101");
    }

    @Test
    void shouldAddOrderByBeforeQueryOrderByWithoutDefaultSort() throws Exception {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithDefaultSortingInQueryAndOptions(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table order by name ASC, id LIMIT 101");
    }

    @Test
    void shouldInjectSortBeforeQueriesLastOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithMultipleOrderBy(collectionOptions).blockFirst();
        mockDb.verifySelect("select name, lastname from customers where name = ANY(select first_name from persons order by first_name) order by name ASC, lastname LIMIT 101");
    }

    @Test
    void shouldFindOrderByWithoutSpaceBefore() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithoutSpaceBeforeOrderBy(collectionOptions).blockFirst();
        mockDb.verifySelect("select * from table\norder by name ASC, id LIMIT 101");
    }

    interface CollectionOptionsDao {
        @Query("select * from table")
        Flux<String> selectWithPaging(CollectionOptions collectionOptions);

        @Query(value = "select * from table", allowedSortColumns = {"name"})
        Flux<String> selectWithSorting(CollectionOptions collectionOptions);

        @Query(value = "select * from table order by id", allowedSortColumns = {"name"})
        Flux<String> selectWithDefaultSorting(CollectionOptions collectionOptions);

        @Query(value = "select * from table order by id", allowedSortColumns = {"name","id"})
        Flux<String> selectWithDefaultSortingAllowMultiple(CollectionOptions collectionOptions);

        @Query(value = "select * from table order by id", allowedSortColumns = {"name"}, defaultSort = "text desc")
        Flux<String> selectWithDefaultSortingInQueryAndOptions(CollectionOptions collectionOptions);

        @Query(value = "select * from table", allowedSortColumns = {"user_name"})
        Flux<String> selectWithSortingCamelCase(CollectionOptions collectionOptions);

        @Query(value = "select * from table", maxLimit = 3)
        Flux<String> selectWithMaxLimit3(CollectionOptions collectionOptions);

        @Query(value = "select * from table", defaultLimit = 1)
        Mono<String> selectWithDefaultLimit1(CollectionOptions collectionOptions);

        @Query(value = "select * from table", defaultLimit = 10)
        Flux<String> selectWithDefaultLimit10(CollectionOptions collectionOptions);

        @Query(value= "select name, lastname from customers where name = ANY(select first_name from persons order by first_name) order by lastname", allowedSortColumns = {"name"})
        Flux<String> selectWithMultipleOrderBy(CollectionOptions collectionOptions);

        @Query(value = "select * from table\norder by id", allowedSortColumns = {"name"})
        Flux<String> selectWithoutSpaceBeforeOrderBy(CollectionOptions collectionOptions);
    }
}
