package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class LocalDateTimeDeserializerTest {
    private final static Deserializer<LocalDateTime> DESERIALIZER = new LocalDateTimeDeserializer();

    @Test
    void shouldDeserialize() throws DeserializerException {
        LocalDateTime deserialized = DESERIALIZER.deserialize("2023-04-01T14:10:15");
        assertThat(deserialized.toString()).isEqualTo("2023-04-01T14:10:15");
    }

    @Test
    void shouldDeserializeNull() throws DeserializerException {
        LocalDateTime deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    void shouldThrowDeserializerExceptionForBadInput() {
        try {
            DESERIALIZER.deserialize("not a datetime");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.localdatetime");
        }
    }
}
