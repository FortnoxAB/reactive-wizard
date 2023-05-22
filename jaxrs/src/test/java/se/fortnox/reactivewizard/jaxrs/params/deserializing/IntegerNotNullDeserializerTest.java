package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class IntegerNotNullDeserializerTest {
    private final static Deserializer<Integer> DESERIALIZER = new IntegerNotNullDeserializer();

    @Test
    void shouldDeserialize() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("5")).isEqualTo(5);
        assertThat(DESERIALIZER.deserialize(String.valueOf(Integer.MIN_VALUE))).isEqualTo(Integer.MIN_VALUE);
        assertThat(DESERIALIZER.deserialize(String.valueOf(Integer.MAX_VALUE))).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void shouldThrowExceptionForNull() {
        try {
            DESERIALIZER.deserialize(null);
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.int");
        }
    }

    @Test
    void shouldThrowDeserializerExceptionForUnparsableStrings() {
        try {
            DESERIALIZER.deserialize("not a recognized value");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.int");
        }
    }
}
