package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.junit.Test;
import se.fortnox.reactivewizard.json.JsonSerializerFactory;

import java.util.function.Function;

import static org.fest.assertions.Assertions.assertThat;

public class LambdaWriterTest {
	@Test
	public void testSerializeAsArray() {
		JsonSerializerFactory jsonSerializerFactory = new JsonSerializerFactory();
		Function<TestEntity, String> stringSerializer = jsonSerializerFactory.createStringSerializer(TestEntity.class);

		assertThat(stringSerializer.apply(new TestEntity())).isEqualTo("[\"hej\"]");
		assertThat(stringSerializer.apply(new TestEntity(){{
			setField(null);
		}})).isEqualTo("[null]");
	}

	@JsonFormat(shape = JsonFormat.Shape.ARRAY)
	class TestEntity {
		private String field = "hej";

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}
}
