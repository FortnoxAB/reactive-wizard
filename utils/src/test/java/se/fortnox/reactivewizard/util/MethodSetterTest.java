package se.fortnox.reactivewizard.util;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class MethodSetterTest {
	@Test
	public void shouldSetValue() throws Exception {
		Setter setter = ReflectionUtil.getSetter(Foo.class, "value");
		Foo foo = new Foo(1);
		setter.invoke(foo, 9);
		assertThat(foo.field).isEqualTo(9);
	}

	private class Foo {
		private int field;

		public Foo(int field) {
			this.field = field;
		}

		public void setValue(int value) {
			this.field = value;
		}
	}

}