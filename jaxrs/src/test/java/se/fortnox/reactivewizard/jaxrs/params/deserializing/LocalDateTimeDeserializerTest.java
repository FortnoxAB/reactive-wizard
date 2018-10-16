package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class LocalDateTimeDeserializerTest {
    private final static Deserializer<LocalDateTime> DESERIALIZER = new LocalDateTimeDeserializer();

    @Test
    public void shouldDeserialize() throws DeserializerException {
        LocalDateTime deserialized = DESERIALIZER.deserialize("2010-01-01T12:34:56");
        assertThat(deserialized.toString()).isEqualTo("2010-01-01T12:34:56");
    }

    @Test
    public void shouldDeserializeTimestampsInMilliseconds() throws DeserializerException {
        LocalDateTime deserialized = DESERIALIZER.deserialize("1262345696000");
        assertThat(deserialized.toString()).isEqualTo("2010-01-01T12:34:56");
    }


    @Test
    public void shouldDeserializeNull() throws DeserializerException {
        LocalDateTime deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    public void shouldThrowDeserializerExceptionForBadDates()  {
        try {
            DESERIALIZER.deserialize("not a date");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.localdatetime");
        }
    }
}