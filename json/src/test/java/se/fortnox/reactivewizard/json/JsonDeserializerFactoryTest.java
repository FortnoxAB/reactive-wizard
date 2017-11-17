package se.fortnox.reactivewizard.json;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import static org.fest.assertions.Assertions.assertThat;

public class JsonDeserializerFactoryTest {
	private JsonSerializerFactory   serializerFactory;
	private JsonDeserializerFactory deserializerFactory;

	private ImmutableEntity immutableEntity;
	private MutableEntity   mutableEntity;
	private Jdk8Types       jdk8Types;

	@Before
	public void setUp() throws Exception {
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
	public void shouldSerializeAndDeserializeImmutableObjects() throws Exception {
		Function<ImmutableEntity, String> serializer = serializerFactory.createStringSerializer(ImmutableEntity.class);
		String json = serializer.apply(immutableEntity);

		Function<String, ImmutableEntity> deserializer = deserializerFactory.createDeserializer(ImmutableEntity.class);
		ImmutableEntity result = deserializer.apply(json);

		assertThat(result).isEqualTo(immutableEntity);
	}

	@Test
	public void shouldSerializeAndDeserializeMutableObjects() throws Exception {
		Function<MutableEntity, String> serializer = serializerFactory.createStringSerializer(MutableEntity.class);
		String json = serializer.apply(mutableEntity);

		Function<String, MutableEntity> deserializer = deserializerFactory.createDeserializer(MutableEntity.class);
		MutableEntity result = deserializer.apply(json);

		assertThat(result).isEqualTo(mutableEntity);
	}

	@Test
	public void shouldSerializeAndDeserializeJdk8Types() throws Exception {
		Function<Jdk8Types, String> serializer = serializerFactory.createStringSerializer(Jdk8Types.class);
		String json = serializer.apply(jdk8Types);

		Function<String, Jdk8Types> deserializer = deserializerFactory.createDeserializer(Jdk8Types.class);
		Jdk8Types result = deserializer.apply(json);

		assertThat(result).isEqualTo(jdk8Types);
	}

	private MutableEntity mutableEntity(String stringProperty, int intProperty) {
		MutableEntity entity = new MutableEntity();
		entity.setStringProperty(stringProperty);
		entity.setIntProperty(intProperty);
		return entity;
	}
}