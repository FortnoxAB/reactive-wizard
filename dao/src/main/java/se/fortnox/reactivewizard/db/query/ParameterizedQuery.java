package se.fortnox.reactivewizard.db.query;

import se.fortnox.reactivewizard.db.query.parts.CollectionOptionsQueryPart;
import se.fortnox.reactivewizard.db.query.parts.DynamicQueryPart;
import se.fortnox.reactivewizard.db.query.parts.ParamQueryPart;
import se.fortnox.reactivewizard.db.query.parts.QueryPart;
import se.fortnox.reactivewizard.db.query.parts.StaticQueryPart;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterizedQuery {

    public static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile("(?<!:):([\\w\\.]+)");

    private final List<QueryPart> queryParts;
    private final String          sql;

    private final String methodName;

    public ParameterizedQuery(String sql, Method method) throws SQLException {
        this.sql = sql;
        this.queryParts = createQueryParts(sql, method);
        this.methodName =  String.format("%s::%s", method.getDeclaringClass().getPackage().getName(), method.getName());
    }

    private List<QueryPart> createQueryParts(String sqlInput, Method method) throws SQLException {
        List<QueryPart>               parts          = new ArrayList<>();
        Map<String, DynamicQueryPart> queryArguments = createQueryArguments(method);

        sqlInput = sqlPreProcess(sqlInput);

        if (sqlInput.indexOf('?') != -1) {
            throw new RuntimeException("Unnamed parameters are not supported: " + sqlInput);
        }

        int     pos     = 0;
        Matcher matcher = NAMED_PARAMETER_PATTERN.matcher(sqlInput);
        if (matcher.find()) {
            do {
                parts.add(new StaticQueryPart(sqlInput.substring(pos, matcher.start(1) - 1)));
                String paramName = matcher.group(1);
                parts.add(getDynamicQueryPart(paramName, queryArguments));
                pos = matcher.end(1);
            }
            while (matcher.find());
        }
        parts.add(new StaticQueryPart(sqlInput.substring(pos, sqlInput.length())));
        parts.add(new CollectionOptionsQueryPart(method));

        return parts;
    }

    private Map<String, DynamicQueryPart> createQueryArguments(Method method) throws SQLException {
        Type[]                        parameterTypes = method.getGenericParameterTypes();
        Parameter[]                   parameters     = method.getParameters();
        Map<String, DynamicQueryPart> queryArguments = new LinkedHashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            String name = parameters[i].isNamePresent() ? parameters[i].getName() : "param" + i;
            queryArguments.put(name, createParamQueryPart(i, parameterTypes[i]));
        }
        return queryArguments;
    }

    protected DynamicQueryPart createParamQueryPart(int parameterIndex, Type parameterType) throws SQLException {
        return new ParamQueryPart(parameterIndex, parameterType);
    }

    protected DynamicQueryPart getDynamicQueryPart(String paramName, Map<String, DynamicQueryPart> queryArguments) throws SQLException {
        String[] paramNameParts = paramName.split("\\.");
        String[] subPath        = Arrays.copyOfRange(paramNameParts, 1, paramNameParts.length);

        DynamicQueryPart queryPart = queryArguments.get(paramNameParts[0]);
        if (queryPart == null) {
            throw new RuntimeException("Query contains placeholder \"" + paramNameParts[0] + "\" but method noes not have such argument");
        }

        if (subPath.length > 0) {
            queryPart = queryPart.subPath(subPath);
        }

        return queryPart;
    }

    /**
     * Builds prepared statement sql.
     *
     * @param arguments the arguments
     * @return sql
     */
    public String buildSql(Object[] arguments) {
        StringBuilder sql = new StringBuilder();
        for (QueryPart part : queryParts) {
            part.visit(sql, arguments);
        }
        return sql.toString();
    }

    /**
     * Builds prepared statement parameter setters.
     *
     * @param args the arguments
     * @return parameters
     */
    public List<ParamSetter> buildParamSetters(Object[] args) {
        var list = new ArrayList<ParamSetter>();
        for (QueryPart part : queryParts) {
            part.addParamSetter(list, args);
        }
        return list;
    }

    protected String sqlPreProcess(String sqlInp) {
        return sqlInp
            .replaceAll("(?i) NOT IN\\s*\\(", " !=ALL\\(")
            .replaceAll("(?i) IN\\s*\\(", " =ANY\\(");
    }

    @Override
    public String toString() {
        return sql;
    }

    public String getMethodName() {
        return methodName;
    }
}
