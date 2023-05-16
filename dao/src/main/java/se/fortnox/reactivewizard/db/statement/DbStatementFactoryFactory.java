package se.fortnox.reactivewizard.db.statement;

import jakarta.inject.Inject;
import se.fortnox.reactivewizard.db.GeneratedKey;
import se.fortnox.reactivewizard.db.Query;
import se.fortnox.reactivewizard.db.Update;
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
            if (annotation instanceof Query) {
                Query queryAnnotation = (Query)annotation;
                return new SelectStatementFactory(createParameterizedQuery(queryAnnotation.value(), method), cls);

            } else if (annotation instanceof Update) {
                Update             updateAnnotation   = (Update)annotation;
                ParameterizedQuery parameterizedQuery = createParameterizedQuery(updateAnnotation.value(), method);

                if (GeneratedKey.class.isAssignableFrom(cls)) {
                    Class<?> keyType = (Class<?>)((ParameterizedType)returnType).getActualTypeArguments()[0];
                    return new UpdateStatementReturningGeneratedKeyFactory(parameterizedQuery, keyType, updateAnnotation.minimumAffected());

                } else if (Integer.class.isAssignableFrom(cls)) {
                    return new UpdateStatementExecutorReturningCountFactory(parameterizedQuery, updateAnnotation.minimumAffected());

                } else if (Void.class.isAssignableFrom(cls)) {
                    return new UpdateStatementReturningVoidFactory(parameterizedQuery, updateAnnotation.minimumAffected());

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
