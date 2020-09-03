package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class EnumDeserializerTest {
    private final static Deserializer<TestEnum> DESERIALIZER = new EnumDeserializer<>(TestEnum.class);

    enum TestEnum {
        FIRST, SECOND
    }

    @Test
    public void shouldDeserialize() throws DeserializerException {
        TestEnum deserialized = DESERIALIZER.deserialize("FIRST");
        assertThat(deserialized).isEqualTo(TestEnum.FIRST);
    }

    @Test
    public void shouldDeserializeNull() throws DeserializerException {
        TestEnum deserialized = DESERIALIZER.deserialize(null);
        assertThat(deserialized).isNull();
    }

    @Test
    public void shouldBeCaseInsensitive() throws DeserializerException {
        TestEnum deserialized = DESERIALIZER.deserialize("first");
        assertThat(deserialized).isEqualTo(TestEnum.FIRST);
    }

    @Test
    public void shouldThrowDeserializerExceptionForUnknownEnums() {
        try {
            DESERIALIZER.deserialize("not a recognized value");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.enum");
        }
    }

}
