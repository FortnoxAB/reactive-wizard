package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class JsonSerializerFactoryTest {

	private JsonSerializerFactory serializerFactory = new JsonSerializerFactory();
	private JsonConfig jsonConfigWithoutLambdaModifier = new JsonConfig();
	{
		jsonConfigWithoutLambdaModifier.setUseLambdaSerializerModifier(false);
	}
	private JsonSerializerFactory serializerFactoryWithoutLambdaModifier = new JsonSerializerFactory(new ObjectMapper(), jsonConfigWithoutLambdaModifier);

    @Test
    void shouldSerializeUsingProtectedProp() {
		PrivateEntity entity = new PrivateEntity();
		entity.setProtectedProp("hello");
		Function<PrivateEntity, String> serializer = serializerFactory.createStringSerializer(PrivateEntity.class);

		assertThat(serializer.apply(entity)).isEqualTo("{\"protectedProp\":\"hello\"}");
	}

    @Test
    void shouldSerializeUsingPrivateClassWithPublicProp() {
		PrivateEntity entity = new PrivateEntity();
		entity.setPublicPropInPrivateClass("hello");
		Function<PrivateEntity, String> serializer = serializerFactory.createStringSerializer(PrivateEntity.class);

		assertThat(serializer.apply(entity)).isEqualTo("{\"publicPropInPrivateClass\":\"hello\"}");
	}

    @Test
    void shouldSerializeUsingFieldProp() {
		PrivateEntity entity = new PrivateEntity();
		entity.fieldProp = "hello";
		Function<PrivateEntity, String> serializer = serializerFactory.createStringSerializer(PrivateEntity.class);

		assertThat(serializer.apply(entity)).isEqualTo("{\"fieldProp\":\"hello\"}");
	}

    @Test
    void shouldThrowInvalidJsonExceptionWhenSerializingFails() {
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
    void shouldReturnNullWhenSerializingNull() {
		for (Function<EntityThrowingOnSerialize, ?> serializer : asList(
				serializerFactory.createStringSerializer(EntityThrowingOnSerialize.class),
				serializerFactory.createByteSerializer(EntityThrowingOnSerialize.class)
		)) {
			assertThat(serializer.apply(null)).isNull();
		}
	}

    @Test
    void shouldSerializeFromType() throws NoSuchMethodException {
		Method method = this.getClass().getDeclaredMethod("methodReturningListOfString");
		Type type = method.getGenericReturnType();
		Function<List<String>, String> serializeList = serializerFactory.createStringSerializer(type);
		assertThat(serializeList.apply(methodReturningListOfString())).isEqualTo("[\"a\",\"b\"]");
	}

    @Test
    void shouldSerializeUnwrapped() {
		var unwrappedEntity = new UnwrappedEntity();
		unwrappedEntity.setEntity(new Entity());
		var serializer = serializerFactoryWithoutLambdaModifier.createStringSerializer(UnwrappedEntity.class);
		assertThat(serializer.apply(unwrappedEntity)).isEqualTo("{\"value\":null}");
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

	private static class UnwrappedEntity {
		@JsonUnwrapped
		private Entity entity;

		public Entity getEntity() {
			return entity;
		}

		public void setEntity(Entity entity) {
			this.entity = entity;
		}
	}

	private static class Entity {
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
