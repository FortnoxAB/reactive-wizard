package se.fortnox.reactivewizard.db.deserializing;

import java.sql.ResultSet;

public interface DbResultSetDeserializer<T> {

    T deserialize(ResultSet rs);
}
