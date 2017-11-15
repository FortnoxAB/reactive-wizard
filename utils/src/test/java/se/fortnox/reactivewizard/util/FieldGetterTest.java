package se.fortnox.reactivewizard.util;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class FieldGetterTest {
	@Test
	public void shouldGetValue() throws Exception {
		Getter getter = ReflectionUtil.getGetter(Foo.class, "value");
		assertThat(getter.invoke(new Foo(5))).isEqualTo(5);
	}

	private class Foo {
		private final int value;

		private Foo(int value) {
			this.value = value;
		}
	}

}