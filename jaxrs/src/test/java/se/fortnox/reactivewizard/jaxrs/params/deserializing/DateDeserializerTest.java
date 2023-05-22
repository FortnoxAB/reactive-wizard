package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class DateDeserializerTest {
    private final static Deserializer<Date> DESERIALIZER = new DateDeserializer(StdDateFormat::new);
    private TimeZone previousTimeZone;

    @BeforeEach
    public void setup() {
        previousTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    public void reset() {
        TimeZone.setDefault(previousTimeZone);
    }

    @Test
    void shouldDeserialize() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize("2010-01-01");
        assertThat(deserialized.toString()).isEqualTo("Fri Jan 01 00:00:00 UTC 2010");
    }

    @Test
    void shouldDeserializeTimestampsInMilliseconds() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize("1262304000000");
        assertThat(deserialized.toString()).isEqualTo("Fri Jan 01 00:00:00 UTC 2010");
    }

    @Test
    void shouldDeserializeNull() throws DeserializerException {
        Date deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    void shouldThrowDeserializerExceptionForBadDates() {
        try {
            DESERIALIZER.deserialize("not a date");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.date");
        }
    }

}
