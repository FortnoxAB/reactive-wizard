package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class UUIDDeserializerTest {
    private final static Deserializer<UUID> DESERIALIZER = new UUIDDeserializer();

    @Test
    void shouldDeserialize() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("b0b895d4-3be8-11e8-b467-0ed5f89f718b").toString()).isEqualTo("b0b895d4-3be8-11e8-b467-0ed5f89f718b");
    }

    @Test
    void shouldNotDeserializeNull() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize(null)).isNull();
    }

    @Test
    void shouldThrowDeserializerExceptionForUnparsableStrings() {
        try {
            DESERIALIZER.deserialize("not a recognized value");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.uuid");
        }
    }
}
