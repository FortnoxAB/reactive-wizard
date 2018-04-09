package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.junit.Test;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DateDeserializerTest {
    private final static Deserializer<Date> DESERIALIZER = new DateDeserializer(StdDateFormat::new);

    @Test
    public void shouldDeserialize() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize("2010-01-01");
        assertThat(deserialized.toString()).isEqualTo("Fri Jan 01 01:00:00 CET 2010");
    }

    @Test
    public void shouldDeserializeTimestampsInMilliseconds() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize("1262304000000");
        assertThat(deserialized.toString()).isEqualTo("Fri Jan 01 01:00:00 CET 2010");
    }

    @Test
    public void shouldDeserializeNull() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    public void shouldThrowDeserializerExceptionForBadDates()  {
        try {
            DESERIALIZER.deserialize("not a date");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.date");
        }
    }

}
