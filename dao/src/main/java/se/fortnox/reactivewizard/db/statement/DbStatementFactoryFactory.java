package se.fortnox.reactivewizard.db.statement;

import jakarta.inject.Inject;
import se.fortnox.reactivewizard.db.GeneratedKey;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.Update;
import se.fortnox.reactivewizard.db.deserializing.DbResultSetDeserializerImpl;
import se.fortnox.reactivewizard.db.query.ParameterizedQuery;
import se.fortnox.reactivewizard.util.ReflectionUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;

public class DbStatementFactoryFactory {

    @Inject
    public DbStatementFactoryFactory() {

    }

    /**
     * Create a statement factory from a method.
     * @param method the method
     * @return the statement factory
     * @throws SQLException if there is an sql error
     */
    public DbStatementFactory createStatementFactory(Method method) throws SQLException {
        Type     returnType = ReflectionUtil.getTypeOfFluxOrMono(method);
        Class<?> cls        = ReflectionUtil.getRawType(returnType);
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof Query queryAnnotation) {
                ParameterizedQuery parameterizedQuery = createParameterizedQuery(queryAnnotation.value(), method);
                var deserializer = new DbResultSetDeserializerImpl<>(cls);
                return new DbStatementFactoryWrapper(
                    parameterizedQuery,
                    (args) -> new SelectSqlStatement<>(
                        parameterizedQuery.buildSql(args),
                        parameterizedQuery.buildParamSetters(args),
                        deserializer,
                        parameterizedQuery.getMethodName(),
                        parameterizedQuery.toString()
                    )
                );

            } else if (annotation instanceof Update updateAnnotation) {
                ParameterizedQuery parameterizedQuery = createParameterizedQuery(updateAnnotation.value(), method);

                if (GeneratedKey.class.isAssignableFrom(cls)) {
                    Class<?> keyType = (Class<?>)((ParameterizedType)returnType).getActualTypeArguments()[0];
                    var deserializer = new DbResultSetDeserializerImpl<>(keyType);
                    return new DbStatementFactoryWrapper(
                        parameterizedQuery,
                        (args) -> new UpdateReturningGeneratedKeySqlStatement<>(
                            parameterizedQuery.buildSql(args),
                            parameterizedQuery.buildParamSetters(args),
                            updateAnnotation.minimumAffected(),
                            parameterizedQuery.toString(),
                            deserializer
                        )
                    );

                } else if (Integer.class.isAssignableFrom(cls)) {
                    return new DbStatementFactoryWrapper(
                        parameterizedQuery,
                        (args) -> new UpdateReturningCountSqlStatement(
                            parameterizedQuery.buildSql(args),
                            parameterizedQuery.buildParamSetters(args),
                            updateAnnotation.minimumAffected(),
                            parameterizedQuery.toString()
                        )
                    );

                } else if (Void.class.isAssignableFrom(cls)) {
                    return new DbStatementFactoryWrapper(
                        parameterizedQuery,
                        (args) -> new UpdateReturningVoidSqlStatement(
                            parameterizedQuery.buildSql(args),
                            parameterizedQuery.buildParamSetters(args),
                            updateAnnotation.minimumAffected(),
                            parameterizedQuery.toString()
                        )
                    );

                } else {
                    throw new RuntimeException("Unsupported return type for Update");
                }
            }
        }
        throw new RuntimeException("Missing annotation @Query or @Update required");

    }

    protected ParameterizedQuery createParameterizedQuery(String query, Method method) throws SQLException {
        return new ParameterizedQuery(query, method);
    }

}
