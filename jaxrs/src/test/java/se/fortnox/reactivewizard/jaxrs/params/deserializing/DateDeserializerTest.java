package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class DateDeserializerTest {
    private final static Deserializer<Date> DESERIALIZER = new DateDeserializer(StdDateFormat::new);
    private TimeZone previousTimeZone;

    @Before
    public void setup() {
        previousTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void reset() {
        TimeZone.setDefault(previousTimeZone);
    }

    @Test
    public void shouldDeserialize() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize("2010-01-01");
        assertThat(deserialized.toString()).isEqualTo("Fri Jan 01 00:00:00 UTC 2010");
    }

    @Test
    public void shouldDeserializeTimestampsInMilliseconds() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize("1262304000000");
        assertThat(deserialized.toString()).isEqualTo("Fri Jan 01 00:00:00 UTC 2010");
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
