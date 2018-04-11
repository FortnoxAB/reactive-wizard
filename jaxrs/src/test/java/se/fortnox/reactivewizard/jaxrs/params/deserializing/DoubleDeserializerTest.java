package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class DoubleDeserializerTest {
    private final static Deserializer<Double> DESERIALIZER = new DoubleDeserializer();

    @Test
    public void shouldDeserializeDouble() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("5")).isEqualTo(5d);
        assertThat(DESERIALIZER.deserialize("7.2")).isEqualTo(7.2d);
        assertThat(DESERIALIZER.deserialize(String.valueOf(Double.MIN_VALUE))).isEqualTo(Double.MIN_VALUE);
        assertThat(DESERIALIZER.deserialize(String.valueOf(Double.MAX_VALUE))).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    public void shouldDeserializeNull() throws DeserializerException {
        Double deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    public void shouldThrowDeserializerExceptionForUnparsableStrings() {
        try {
            DESERIALIZER.deserialize("not a recognized value");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.double");
        }
    }
}
