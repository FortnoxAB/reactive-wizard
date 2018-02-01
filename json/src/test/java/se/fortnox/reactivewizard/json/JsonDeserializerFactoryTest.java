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

	@Test
	public void shouldSerializeAndDeserializeSubClassAttributes() {
		Parent p = new Child();
		assertThat(serializeParent(p)).isEqualTo("{\"a\":\"parent\",\"b\":\"child\"}");

		p = deserializeParent(Child.class, "{\"a\":\"parent\",\"b\":\"child\"}");
		Child c = (Child) p;
		assertThat(c.getA()).isEqualTo("parent");
		assertThat(c.getB()).isEqualTo("child");

		p = deserializeParent(Parent.class, "{\"a\":\"parent\",\"b\":\"child\"}");
		assertThat(p.getA()).isEqualTo("parent");
		assertThat(p instanceof Child).isFalse();
	}

	private <T extends Parent> String serializeParent(T parent) {
		return serializerFactory.createStringSerializer(parent.getClass()).apply(parent);
	}

	private <T extends Parent> T deserializeParent(Class<T> clazz, String json) {
		Function<String, T> deserializer = deserializerFactory.createDeserializer(clazz);
		return deserializer.apply(json);
	}

	private MutableEntity mutableEntity(String stringProperty, int intProperty) {
		MutableEntity entity = new MutableEntity();
		entity.setStringProperty(stringProperty);
		entity.setIntProperty(intProperty);
		return entity;
	}

	static class Parent {
		String a = "parent";
		public String getA() {
			return a;
		}
	}

	static class Child extends Parent {
		String b = "child";
		public String getB() {
			return b;
		}
	}
}