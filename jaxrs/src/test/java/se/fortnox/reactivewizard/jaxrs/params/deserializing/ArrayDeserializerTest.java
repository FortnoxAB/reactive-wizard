package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ArrayDeserializerTest {
    private final static Deserializer<String[]> DESERIALIZER = new ArrayDeserializer((str) -> str, String.class);

    @Test
    void shouldDeserializeCommaSeparatedStrings() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("entry")).hasSize(1);
        assertThat(DESERIALIZER.deserialize("entry,second,third")).hasSize(3);
    }

    @Test
    void shouldHandleDeserializationErrorsForEntries() {
        Deserializer<Boolean[]> longDeserializer = new ArrayDeserializer(new LongDeserializer(), Long.class);
        try {
            longDeserializer.deserialize("5,a,5");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.long");
        }
    }

    @Test
    void shouldDeserializeNull() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize(null)).isNull();
    }
}
