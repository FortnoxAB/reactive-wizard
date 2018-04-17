package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class BooleanDeserializerTest {
    private final static Deserializer<Boolean> DESERIALIZER = new BooleanDeserializer();

    @Test
    public void shouldDeserializeTrue() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("true")).isEqualTo(true);
        assertThat(DESERIALIZER.deserialize("TRUE")).isEqualTo(true);
        assertThat(DESERIALIZER.deserialize("True")).isEqualTo(true);
    }

    @Test
    public void shouldDeserializeFalse() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("false")).isEqualTo(false);
        assertThat(DESERIALIZER.deserialize("FALSE")).isEqualTo(false);
        assertThat(DESERIALIZER.deserialize("False")).isEqualTo(false);
    }

    @Test
    public void shouldDeserializeUnknownValuesToFalse() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("anything")).isEqualTo(false);
    }

    @Test
    public void shouldDeserializeNull() throws DeserializerException {
        Boolean deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }
}
