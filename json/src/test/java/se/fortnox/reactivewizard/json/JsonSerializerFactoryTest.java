package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class JsonSerializerFactoryTest {

	private JsonSerializerFactory   serializerFactory = new JsonSerializerFactory();

	@Test
	public void shouldSerializeUsingProtectedProp() {
		PrivateEntity entity = new PrivateEntity();
		entity.setProtectedProp("hello");
		Function<PrivateEntity, String> serializer = serializerFactory.createStringSerializer(PrivateEntity.class);

		assertThat(serializer.apply(entity)).isEqualTo("{\"protectedProp\":\"hello\"}");
	}

	@Test
	public void shouldSerializeUsingPrivateClassWithPublicProp() {
		PrivateEntity entity = new PrivateEntity();
		entity.setPublicPropInPrivateClass("hello");
		Function<PrivateEntity, String> serializer = serializerFactory.createStringSerializer(PrivateEntity.class);

		assertThat(serializer.apply(entity)).isEqualTo("{\"publicPropInPrivateClass\":\"hello\"}");
	}

	@Test
	public void shouldSerializeUsingFieldProp() {
		PrivateEntity entity = new PrivateEntity();
		entity.fieldProp = "hello";
		Function<PrivateEntity, String> serializer = serializerFactory.createStringSerializer(PrivateEntity.class);

		assertThat(serializer.apply(entity)).isEqualTo("{\"fieldProp\":\"hello\"}");
	}

	@Test
	public void shouldThrowInvalidJsonExceptionWhenSerializingFails() {
		for (Function<EntityThrowingOnSerialize, ?> serializer : asList(
				serializerFactory.createStringSerializer(EntityThrowingOnSerialize.class),
				serializerFactory.createByteSerializer(EntityThrowingOnSerialize.class)
		)) {
			try {
				serializer.apply(new EntityThrowingOnSerialize());
				fail("expected exception");
			} catch(InvalidJsonException e) {
				assertThat(e.getCause()).isInstanceOf(JsonMappingException.class);
				assertThat(e.getCause().getCause()).isInstanceOf(RuntimeException.class);
			}
		}
	}

	@Test
	public void shouldReturnNullWhenSerializingNull() {
		for (Function<EntityThrowingOnSerialize, ?> serializer : asList(
				serializerFactory.createStringSerializer(EntityThrowingOnSerialize.class),
				serializerFactory.createByteSerializer(EntityThrowingOnSerialize.class)
		)) {
			assertThat(serializer.apply(null)).isNull();
		}
	}

	@Test
	public void shouldSerializeFromType() throws NoSuchMethodException {
		Method method = this.getClass().getDeclaredMethod("methodReturningListOfString");
		Type type = method.getGenericReturnType();
		Function<List<String>, String> serializeList = serializerFactory.createStringSerializer(type);
		assertThat(serializeList.apply(methodReturningListOfString())).isEqualTo("[\"a\",\"b\"]");
	}

	private List<String> methodReturningListOfString() {
		return asList("a", "b");
	}

	private static class EntityThrowingOnSerialize {
		private int value;

		public int getValue() {
			throw new RuntimeException();
		}

		public void setValue(int value) {
			this.value = value;
		}
	}
}
