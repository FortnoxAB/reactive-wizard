package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class JsonDeserializerFactoryTest {
    private JsonSerializerFactory   serializerFactory;
    private JsonDeserializerFactory deserializerFactory;

    private ImmutableEntity immutableEntity;
    private MutableEntity   mutableEntity;
    private Jdk8Types       jdk8Types;

    private TypeReference<ImmutableEntity> immutableEntityTypeReference = new TypeReference<ImmutableEntity>() {
        @Override
        public Type getType() {
            return ImmutableEntity.class;
        }
    };

    @Before
    public void setUp() {
        serializerFactory = new JsonSerializerFactory();
        deserializerFactory = new JsonDeserializerFactory();

        immutableEntity = new ImmutableEntity("foo", 5);
        mutableEntity = mutableEntity("bar", 10);
        jdk8Types = new Jdk8Types(LocalDate.of(2017, 1, 1),
            LocalTime.of(13, 37),
            LocalDateTime.of(2017, 1, 1, 13, 37),
            Optional.of("foo"),
            OptionalInt.of(15));
    }

    @Test
    public void shouldThrowInvalidJsonException() {
        Function<String, ImmutableEntity> deserializer = deserializerFactory.createDeserializer(ImmutableEntity.class);

        try {
            deserializer.apply("not real json");
            fail("Expected exception, but none was thrown");
        } catch(Exception actualException) {
            assertThat(actualException).isInstanceOf(InvalidJsonException.class);
            assertThat(actualException.getMessage()).contains("Unrecognized token");
        }
    }

    @Test
    public void shouldThrowInvalidJsonExceptionUsingByteArayDeserializer() {
        Function<byte[], ImmutableEntity> deserializer = deserializerFactory.createByteDeserializer(ImmutableEntity.class);

        try {
            deserializer.apply("not real json".getBytes());
            fail("Expected exception, but none was thrown");
        } catch(Exception actualException) {
            assertThat(actualException).isInstanceOf(InvalidJsonException.class);
            assertThat(actualException.getMessage()).contains("Unrecognized token");
        }
    }

    @Test
    public void shouldDeserializeNullToNull() {
        Function<String, ImmutableEntity> deserializer = deserializerFactory.createDeserializer(ImmutableEntity.class);

        ImmutableEntity result = deserializer.apply(null);

        assertThat(result).isNull();
    }

    @Test
    public void shouldDeserializeByteArrayNullToNull() {
        Function<byte[], ImmutableEntity> deserializer = deserializerFactory.createByteDeserializer(ImmutableEntity.class);

        ImmutableEntity result = deserializer.apply(null);

        assertThat(result).isNull();
    }


    @Test
    public void shouldSerializeAndDeserializeImmutableObjectsByTypeReference() {
        Function<ImmutableEntity, byte[]> serializer = serializerFactory.createByteSerializer(immutableEntityTypeReference);
        byte[] json = serializer.apply(immutableEntity);

        Function<byte[], ImmutableEntity> deserializer = deserializerFactory.createByteDeserializer(immutableEntityTypeReference);
        ImmutableEntity result = deserializer.apply(json);

        assertThat(result).isEqualTo(immutableEntity);
    }


    @Test
    public void shouldSerializeAndDeserializeImmutableObjectsByTypeReferenceToAndFromByteArrays() {
        Function<ImmutableEntity, String> serializer = serializerFactory.createStringSerializer(immutableEntityTypeReference);
        String json = serializer.apply(immutableEntity);

        Function<String, ImmutableEntity> deserializer = deserializerFactory.createDeserializer(immutableEntityTypeReference);
        ImmutableEntity result = deserializer.apply(json);

        assertThat(result).isEqualTo(immutableEntity);
    }

    @Test
    public void shouldSerializeAndDeserializeToAndFromByteArrays() {
        Function<ImmutableEntity, byte[]> serializer = serializerFactory.createByteSerializer(ImmutableEntity.class);
        byte[] json = serializer.apply(immutableEntity);

        Function<byte[], ImmutableEntity> deserializer = deserializerFactory.createByteDeserializer(ImmutableEntity.class);
        ImmutableEntity result = deserializer.apply(json);

        assertThat(result).isEqualTo(immutableEntity);
    }

    @Test
    public void shouldSerializeAndDeserializeImmutableObjects() {
        Function<ImmutableEntity, String> serializer = serializerFactory.createStringSerializer(ImmutableEntity.class);
        String json = serializer.apply(immutableEntity);

        Function<String, ImmutableEntity> deserializer = deserializerFactory.createDeserializer(ImmutableEntity.class);
        ImmutableEntity result = deserializer.apply(json);

        assertThat(result).isEqualTo(immutableEntity);
    }

    @Test
    public void shouldSerializeAndDeserializeMutableObjects() {
        Function<MutableEntity, String> serializer = serializerFactory.createStringSerializer(MutableEntity.class);
        String json = serializer.apply(mutableEntity);

        Function<String, MutableEntity> deserializer = deserializerFactory.createDeserializer(MutableEntity.class);
        MutableEntity result = deserializer.apply(json);

        assertThat(result).isEqualTo(mutableEntity);
    }

    @Test
    public void shouldSerializeAndDeserializeJdk8Types() {
        Function<Jdk8Types, String> serializer = serializerFactory.createStringSerializer(Jdk8Types.class);
        String json = serializer.apply(jdk8Types);

        Function<String, Jdk8Types> deserializer = deserializerFactory.createDeserializer(Jdk8Types.class);
        Jdk8Types result = deserializer.apply(json);

        assertThat(result).isEqualTo(jdk8Types);
    }

    @Test
    public void shouldDeserializeFromType() throws NoSuchMethodException {
        Method method = this.getClass().getDeclaredMethod("methodReturningListOfString");
        Type type = method.getGenericReturnType();
        Function<String, List<String>> serializeList = deserializerFactory.createDeserializer(type);
        List<String> result = serializeList.apply("[\"a\",\"b\"]");
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo("a");
        assertThat(result.get(1)).isEqualTo("b");
    }

    private List<String> methodReturningListOfString() {
        return asList("a", "b");
    }


    private MutableEntity mutableEntity(String stringProperty, int intProperty) {
        MutableEntity entity = new MutableEntity();
        entity.setStringProperty(stringProperty);
        entity.setIntProperty(intProperty);
        return entity;
    }
}
