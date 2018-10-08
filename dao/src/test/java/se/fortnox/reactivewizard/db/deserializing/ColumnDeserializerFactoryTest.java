package se.fortnox.reactivewizard.db.deserializing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ColumnDeserializerFactoryTest {
    @Mock
    private ResultSet resultSet;

    @Test
    public void shouldDeserializeToBigDecimal() throws Exception {
        when(resultSet.getBigDecimal(0)).thenReturn(BigDecimal.TEN);
        Deserializer deserializer = ColumnDeserializerFactory.getColumnDeserializer(BigDecimal.class, Types.NUMERIC, 0);
        assertThat(deserializer.deserialize(resultSet)).isEqualTo(Optional.of(BigDecimal.TEN));
    }
}