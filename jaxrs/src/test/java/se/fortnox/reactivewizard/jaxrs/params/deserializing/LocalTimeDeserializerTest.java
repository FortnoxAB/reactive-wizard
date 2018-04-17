package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.Test;

import java.time.LocalTime;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class LocalTimeDeserializerTest {
    private final static Deserializer<LocalTime> DESERIALIZER = new LocalTimeDeserializer();

    @Test
    public void shouldDeserialize() throws DeserializerException {
        LocalTime deserialized = DESERIALIZER.deserialize("14:10:15");
        assertThat(deserialized.toString()).isEqualTo("14:10:15");
    }

    @Test
    public void shouldDeserializeNull() throws DeserializerException {
        LocalTime deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    public void shouldThrowDeserializerExceptionForBadDates() {
        try {
            DESERIALIZER.deserialize("not a date");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.localtime");
        }
    }

}
