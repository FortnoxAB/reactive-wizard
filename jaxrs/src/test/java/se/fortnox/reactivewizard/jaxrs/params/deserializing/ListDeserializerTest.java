package se.fortnox.reactivewizard.jaxrs.params.deserializing;

import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ListDeserializerTest {
    private final static Deserializer<List<String>> DESERIALIZER = new ListDeserializer((str) -> str);

    @Test
    public void shouldDeserializeCommaSeparatedStrings() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("entry")).hasSize(1);
        assertThat(DESERIALIZER.deserialize("entry,second,third")).hasSize(3);
    }

    @Test
    public void shouldDeserializeEmptyLists() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize("")).hasSize(0);
    }

    @Test
    public void shouldHandleDeserializationErrorsForEntries() {
        Deserializer<Boolean[]> longDeserializer = new ArrayDeserializer(new LongDeserializer(), Long.class);
        try {
            longDeserializer.deserialize("5,notlong,5");
            fail("Expected exception, but none was thrown");
        } catch (Exception exception) {
            assertThat(exception).isInstanceOf(DeserializerException.class);
            assertThat(exception.getMessage()).isEqualTo("invalid.long");
        }
    }

    @Test
    public void shouldDeserializeNull() throws DeserializerException {
        assertThat(DESERIALIZER.deserialize(null)).isNull();
    }
}
