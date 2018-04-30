package se.fortnox.reactivewizard.db.query.parts;

import se.fortnox.reactivewizard.CollectionOptions;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.query.PreparedStatementParameters;
import se.fortnox.reactivewizard.util.CamelSnakeConverter;

import java.lang.reflect.Method;
import java.sql.SQLException;

import static com.google.common.collect.Iterables.indexOf;
import static java.util.Arrays.asList;

public class CollectionOptionsQueryPart implements QueryPart {
    private static final String ORDER_BY = " ORDER BY ";
    private final        int    collectionOptionsArgIndex;
    private final        Query  queryAnnotation;

    public CollectionOptionsQueryPart(Method method) {
        collectionOptionsArgIndex = indexOf(asList(method.getParameterTypes()), CollectionOptions.class::isAssignableFrom);

        queryAnnotation = method.getDeclaredAnnotation(Query.class);
    }

    @Override
    public void visit(StringBuilder sql, Object[] args) {
        if (collectionOptionsArgIndex != -1 && queryAnnotation != null) {
            CollectionOptions collectionOptions           = (CollectionOptions)args[collectionOptionsArgIndex];
            boolean           collectionOptionsHasOrderBy = false;
            Integer           limit                       = null;
            Integer           offset                      = null;
            if (collectionOptions != null) {
                if (collectionOptions.getSortBy() != null) {
                    String sortBy = CamelSnakeConverter.camelToSnake(collectionOptions.getSortBy());
                    for (String allowed : queryAnnotation.allowedSortColumns()) {
                        if (allowed.equals(sortBy)) {
                            collectionOptionsHasOrderBy = true;
                            addOrderBy(sql, buildOrderBy(collectionOptions.getOrder(), allowed));
                            break;
                        }
                    }
                }
                limit = collectionOptions.getLimit();
                offset = collectionOptions.getOffset();
            }

            if (!collectionOptionsHasOrderBy && !queryAnnotation.defaultSort().isEmpty()) {
                addOrderBy(sql, queryAnnotation.defaultSort());
            }

            if (limit == null || limit < 0) {
                limit = queryAnnotation.defaultLimit();
            }
            if (limit > queryAnnotation.maxLimit()) {
                limit = queryAnnotation.maxLimit();
            }

            // Add 1 to limit, to get one more record than requested, so
            // that we know for sure if there are more records, in case we
            // get exactly the same amount back as was requested. The extra
            // record is thrown away in PagingResponseProcessor
            limit = limit + 1;
            sql.append(" LIMIT ");
            sql.append(limit);

            if (offset != null && offset > 0) {
                sql.append(" OFFSET ");
                sql.append(offset);
            }
        }
    }

    private String buildOrderBy(CollectionOptions.SortOrder order, String column) {
        StringBuilder orderBy = new StringBuilder(column);
        if (order != null) {
            orderBy.append(" ");
            orderBy.append(order.toString());
        }
        return orderBy.toString();
    }

    private void addOrderBy(StringBuilder sql, String orderBy) {
        StringBuilder orderByClause = new StringBuilder(orderBy);
        int           pos           = sql.toString().toUpperCase().indexOf(ORDER_BY);
        if (pos != -1) {
            pos += ORDER_BY.length();
            orderByClause.append(", ");
            sql.insert(pos, orderByClause);
        } else {
            sql.append(ORDER_BY);
            sql.append(orderByClause);
        }
    }

    @Override
    public void addParams(PreparedStatementParameters preparedStatement, Object[] args) throws SQLException {
    }
}
