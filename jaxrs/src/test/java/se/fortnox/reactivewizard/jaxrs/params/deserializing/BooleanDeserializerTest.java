package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BooleanDeserializerTest {
    private final static Deserializer<Boolean> DESERIALIZER = new BooleanDeserializer();

    @Test
    void shouldDeserializeTrue() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("true")).isEqualTo(true);
        assertThat(DESERIALIZER.deserialize("TRUE")).isEqualTo(true);
        assertThat(DESERIALIZER.deserialize("True")).isEqualTo(true);
    }

    @Test
    void shouldDeserializeFalse() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("false")).isEqualTo(false);
        assertThat(DESERIALIZER.deserialize("FALSE")).isEqualTo(false);
        assertThat(DESERIALIZER.deserialize("False")).isEqualTo(false);
    }

    @Test
    void shouldDeserializeUnknownValuesToFalse() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("anything")).isEqualTo(false);
    }

    @Test
    void shouldDeserializeNull() throws DeserializerException {
        Boolean deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }
}
