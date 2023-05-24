package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class LocalDateDeserializerTest {
    private final static Deserializer<LocalDate> DESERIALIZER = new LocalDateDeserializer();

    @Test
    void shouldDeserialize() throws DeserializerException {
        LocalDate deserialized = DESERIALIZER.deserialize("2010-01-01");
        assertThat(deserialized.toString()).isEqualTo("2010-01-01");
    }

    @Test
    void shouldDeserializeTimestampsInMilliseconds() throws DeserializerException {
        LocalDate deserialized = DESERIALIZER.deserialize("1262304000000");
        assertThat(deserialized.toString()).isEqualTo("2010-01-01");
    }


    @Test
    void shouldDeserializeNull() throws DeserializerException {
        LocalDate deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }


    @Test
    void shouldThrowDeserializerExceptionForBadDates() {
        try {
            DESERIALIZER.deserialize("not a date");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.localdate");
        }
    }

}
