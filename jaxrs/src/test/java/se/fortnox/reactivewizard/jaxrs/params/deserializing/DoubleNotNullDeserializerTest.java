package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class DoubleNotNullDeserializerTest {
    private final static Deserializer<Double> DESERIALIZER = new DoubleNotNullDeserializer();

    @Test
    void shouldDeserializeDouble() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("5")).isEqualTo(5d);
        assertThat(DESERIALIZER.deserialize("7.2")).isEqualTo(7.2d);
        assertThat(DESERIALIZER.deserialize(String.valueOf(Double.MIN_VALUE))).isEqualTo(Double.MIN_VALUE);
        assertThat(DESERIALIZER.deserialize(String.valueOf(Double.MAX_VALUE))).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    void shouldThrowExceptionForNull() {
        try {
            DESERIALIZER.deserialize(null);
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.double");
        }
    }

    @Test
    void shouldThrowDeserializerExceptionForUnparsableStrings() {
        try {
            DESERIALIZER.deserialize("not a recognized value");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.double");
        }
    }
}
