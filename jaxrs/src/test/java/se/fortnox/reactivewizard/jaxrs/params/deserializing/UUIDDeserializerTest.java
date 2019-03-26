package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class UUIDDeserializerTest {
    private final static Deserializer<UUID> DESERIALIZER = new UUIDDeserializer();

    @Test
    public void shouldDeserialize() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("b0b895d4-3be8-11e8-b467-0ed5f89f718b").toString()).isEqualTo("b0b895d4-3be8-11e8-b467-0ed5f89f718b");
    }

    @Test
    public void shouldNotDeserializeNull() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize(null)).isNull();
    }

    @Test
    public void shouldThrowDeserializerExceptionForUnparsableStrings() {
        try {
            DESERIALIZER.deserialize("not a recognized value");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.uuid");
        }
    }
}
