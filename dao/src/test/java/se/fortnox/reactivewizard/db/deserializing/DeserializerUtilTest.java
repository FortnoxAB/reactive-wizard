package se.fortnox.reactivewizard.db.deserializing;

import org.apache.log4j.Appender;
import org.h2.tools.SimpleResultSet;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static se.fortnox.reactivewizard.test.LoggingMockUtil.createMockedLogAppender;
import static se.fortnox.reactivewizard.test.LoggingMockUtil.destroyMockedAppender;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

public class DeserializerUtilTest {
    @Test
    public void shouldLogWarnIfNoPropertyDeserializerWasFound() throws SQLException, NoSuchFieldException, IllegalAccessException {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("test_a", Types.VARCHAR, 255, 0);

        Appender mockAppender = createMockedLogAppender(DeserializerUtil.class);
        DeserializerUtil.createPropertyDeserializers(ClassWithoutPropertyA.class, resultSet, (propertyResolver, deserializer) -> deserializer);

        verify(mockAppender).doAppend(matches(log -> {
            assertThat(log.getLevel().toString()).isEqualTo("WARN");
            assertThat(log.getMessage().toString())
                .matches("Tried to deserialize column test_a, but found no matching property named testA in ClassWithoutPropertyA");
        }));
        destroyMockedAppender(mockAppender, DeserializerUtil.class);
    }

    @Test
    public void shouldNotWarnForFoundProperties() throws SQLException, NoSuchFieldException, IllegalAccessException {
        SimpleResultSet resultSet = new SimpleResultSet();
        resultSet.addColumn("test_b", Types.VARCHAR, 255, 0);

        Appender mockAppender = createMockedLogAppender(DeserializerUtil.class);

        DeserializerUtil.createPropertyDeserializers(ClassWithoutPropertyA.class, resultSet, (propertyResolver, deserializer) -> deserializer);

        verify(mockAppender, never()).doAppend(any());
        destroyMockedAppender(mockAppender, DeserializerUtil.class);
    }
}

class ClassWithoutPropertyA {
    String testB;
}
