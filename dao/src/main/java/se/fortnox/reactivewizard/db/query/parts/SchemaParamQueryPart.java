package se.fortnox.reactivewizard.db.query.parts;

import se.fortnox.reactivewizard.db.query.PreparedStatementParameters;

import java.lang.reflect.Type;
import java.sql.SQLException;

public class SchemaParamQueryPart extends ParamQueryPart {

    private final String argName;
    private       String tableName = "";

    public SchemaParamQueryPart(String argName, int paramIndex, Type parameterType)
        throws SQLException {
        super(paramIndex, parameterType);
        this.argName = argName;
    }

    @Override
    public void visit(StringBuilder sql, Object[] args) {
        Object schemaName = getValue(args);
        if (schemaName == null) {
            throw new NullPointerException("Schema must not be null: " + argName);
        }
        sql.append('"');
        sql.append(schemaName);
        sql.append("\".");
        sql.append(tableName);
    }

    @Override
    public DynamicQueryPart subPath(String[] subPath) throws SQLException {
        this.tableName = String.join(".", subPath);
        return this;
    }

    @Override
    public void addParams(PreparedStatementParameters parameters, Object[] args) throws SQLException {

    }
}
