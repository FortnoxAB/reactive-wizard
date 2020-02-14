package se.fortnox.reactivewizard.db;

import org.junit.Test;
import rx.Observable;
import se.fortnox.reactivewizard.CollectionOptions;
import se.fortnox.reactivewizard.CollectionOptions.SortOrder;
import se.fortnox.reactivewizard.db.config.DatabaseConfig;
import se.fortnox.reactivewizard.db.paging.CollectionOptionsWithResult;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 */
public class CollectionOptionsTest {
    MockDb               mockDb               = new MockDb();
    DbProxy              proxy                = new DbProxy(new DatabaseConfig(), mockDb.getConnectionProvider());
    CollectionOptionsDao collectionOptionsDao = proxy.create(CollectionOptionsDao.class);

    @Test
    public void shouldAddLimitFromCollectionOptions() throws SQLException {
        mockDb.addRows(4);
        assertThat(collectionOptionsDao.selectWithPaging(new CollectionOptions(3, null)).toList().toBlocking().single()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4");
    }

    @Test
    public void shouldAddOffsetFromCollectionOptions() throws SQLException {
        mockDb.addRows(3);
        assertThat(collectionOptionsDao.selectWithPaging(new CollectionOptions(null, 3)).toList().toBlocking().single()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 101 OFFSET 3");
    }

    @Test
    public void shouldAddLimitAndOffsetFromCollectionOptions() throws SQLException {
        mockDb.addRows(4);
        assertThat(collectionOptionsDao.selectWithPaging(new CollectionOptions(3, 3)).toList().toBlocking().single()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 3");
    }

    @Test
    public void shouldNotSetOffsetIfNegative() throws SQLException {
        collectionOptionsDao.selectWithPaging(new CollectionOptions(2, -1)).toList().toBlocking().single();
        mockDb.verifySelect("select * from table LIMIT 3");
    }

    @Test
    public void shouldNotSetLimitIfNegative() throws SQLException {
        collectionOptionsDao.selectWithPaging(new CollectionOptions(-1, 2)).toList().toBlocking().single();
        mockDb.verifySelect("select * from table LIMIT 101 OFFSET 2");
    }

    @Test
    public void shouldNotSetLimitAboveMaxLimit() throws SQLException {
        collectionOptionsDao.selectWithMaxLimit3(new CollectionOptions(5, 2)).toList().toBlocking().single();
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 2");
    }

    @Test
    public void shouldUseConfiguredDefaultLimit() throws SQLException {
        collectionOptionsDao.selectWithDefaultLimit10(null).toList().toBlocking().single();
        mockDb.verifySelect("select * from table LIMIT 11");
    }

    @Test
    public void shouldSetLastRecordToFalseIfThereAreMoreRows() throws SQLException {
        mockDb.addRows(4);
        CollectionOptionsWithResult CollectionOptions = new CollectionOptionsWithResult(3, 3);
        assertThat(collectionOptionsDao.selectWithPaging(CollectionOptions).toList().toBlocking().single()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 3");
        assertThat(CollectionOptions.isLastRecord()).isFalse();
    }

    @Test
    public void shouldSetLastRecordToTrueIfThereAreNoMoreRows() throws SQLException {
        mockDb.addRows(3);
        CollectionOptionsWithResult CollectionOptions = new CollectionOptionsWithResult(3, 3);
        assertThat(collectionOptionsDao.selectWithPaging(CollectionOptions).toList().toBlocking().single()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4 OFFSET 3");
        assertThat(CollectionOptions.isLastRecord()).isTrue();
    }

    @Test
    public void shouldNotAddCollectionOptionsAsParameter() throws SQLException {
        mockDb.addRows(4);
        assertThat(collectionOptionsDao.selectWithPaging(new CollectionOptions(3, null)).toList().toBlocking().single()).hasSize(3);
        mockDb.verifySelect("select * from table LIMIT 4");
        verify(mockDb.getPreparedStatement(), never()).setObject(anyInt(), any());
    }

    @Test
    public void shouldAddOrderByAsc() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithSorting(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table ORDER BY name ASC LIMIT 101");
    }

    @Test
    public void shouldAddOrderByDesc() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.DESC);
        collectionOptionsDao.selectWithSorting(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table ORDER BY name DESC LIMIT 101");
    }

    @Test
    public void shouldNotAddOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("badcolumn", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithSorting(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table LIMIT 101");
    }

    @Test
    public void shouldAddOrderByAndLimit() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions(1, null, "name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithSorting(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table ORDER BY name ASC LIMIT 2");
    }

    @Test
    public void shouldConvertCamelCaseToSnakeCase() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("userName", CollectionOptions.SortOrder.DESC);
        collectionOptionsDao.selectWithSortingCamelCase(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table ORDER BY user_name DESC LIMIT 101");
    }

    @Test
    public void shouldAddOptionsOrderByBeforeQueryOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithDefaultSorting(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table order by name ASC, id LIMIT 101");
    }

    @Test
    public void shouldAddDefaultSortBeforeQueryOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions();
        collectionOptionsDao.selectWithDefaultSortingInQueryAndOptions(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table order by text desc, id LIMIT 101");
    }

    @Test
    public void shouldAddOrderByBeforeQueryOrderByWithoutDefaultSort() throws Exception {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithDefaultSortingInQueryAndOptions(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select * from table order by name ASC, id LIMIT 101");
    }

    @Test
    public void shouldInjectSortBeforeQueriesLastOrderBy() throws SQLException {
        CollectionOptions collectionOptions = new CollectionOptions("name", CollectionOptions.SortOrder.ASC);
        collectionOptionsDao.selectWithMultipleOrderBy(collectionOptions).toBlocking().singleOrDefault(null);
        mockDb.verifySelect("select name, lastname from customers where name = ANY(select first_name from persons order by first_name) order by name ASC, lastname LIMIT 101");
    }

    interface CollectionOptionsDao {
        @Query("select * from table")
        Observable<String> selectWithPaging(CollectionOptions collectionOptions);

        @Query(value = "select * from table", allowedSortColumns = {"name"})
        Observable<String> selectWithSorting(CollectionOptions collectionOptions);

        @Query(value = "select * from table order by id", allowedSortColumns = {"name"})
        Observable<String> selectWithDefaultSorting(CollectionOptions collectionOptions);

        @Query(value = "select * from table order by id", allowedSortColumns = {"name"}, defaultSort = "text desc")
        Observable<String> selectWithDefaultSortingInQueryAndOptions(CollectionOptions collectionOptions);

        @Query(value = "select * from table", allowedSortColumns = {"user_name"})
        Observable<String> selectWithSortingCamelCase(CollectionOptions collectionOptions);

        @Query(value = "select * from table", maxLimit = 3)
        Observable<String> selectWithMaxLimit3(CollectionOptions collectionOptions);

        @Query(value = "select * from table", defaultLimit = 10)
        Observable<String> selectWithDefaultLimit10(CollectionOptions collectionOptions);

        @Query(value= "select name, lastname from customers where name = ANY(select first_name from persons order by first_name) order by lastname", allowedSortColumns = {"name"})
        Observable<String> selectWithMultipleOrderBy(CollectionOptions collectionOptions);
    }
}
