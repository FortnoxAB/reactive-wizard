package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class BigDecimalDeserializerTest {
    private final static Deserializer<BigDecimal> DESERIALIZER = new BigDecimalDeserializer();

    @Test
    void shouldDeserializeBigDecimal() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("5")).isEqualTo(BigDecimal.valueOf(5));
        assertThat(DESERIALIZER.deserialize("7.2")).isEqualTo(BigDecimal.valueOf(7.2d));
        assertThat(DESERIALIZER.deserialize("1234.56789")).isEqualTo(new BigDecimal("1234.56789"));
    }

    @Test
    void shouldDeserializeNull() throws DeserializerException {
        BigDecimal deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    void shouldThrowDeserializerExceptionForUnparsableStrings() {
        try {
            DESERIALIZER.deserialize("not a recognized value");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.bigdecimal");
        }
    }
}
