package se.fortnox.reactivewizard.db.deserializing;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static se.fortnox.reactivewizard.db.deserializing.MutabilityDetector.isImmutable;

public class DbResultSetDeserializerImpl {
    private final Class<?>     cls;
    private       Deserializer deserializer;

    public DbResultSetDeserializerImpl(Class<?> cls) {
        this.cls = cls;
    }

    /**
     * Deserialize a ResultSet.
     * @param rs the ResultSet
     * @return the result
     */
    public Object deserialize(ResultSet rs) {
        try {
            if (deserializer == null) {
                deserializer = createDeserializer(cls, rs);
            }
            return deserializer.deserialize(rs).orElse(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Deserializer createDeserializer(Class<?> cls, ResultSet recordSet) throws SQLException {
        ResultSetMetaData metaData = recordSet.getMetaData();
        if (deserializer == null) {
            deserializer = ColumnDeserializerFactory.getColumnDeserializer(cls, recordSet.getMetaData().getColumnType(1), 1);
            if (deserializer == null) {
                deserializer = createDeserializer(cls, metaData);
            }
        }
        return deserializer;
    }

    private Deserializer createDeserializer(Class<?> cls, ResultSetMetaData metaData) throws SQLException {
        return isImmutable(cls) ?
            JacksonObjectDeserializer.create(cls, metaData) :
            SimpleObjectDeserializer.create(cls, metaData);
    }
}
