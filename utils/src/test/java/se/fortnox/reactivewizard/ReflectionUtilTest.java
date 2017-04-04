package se.fortnox.reactivewizard;

import se.fortnox.reactivewizard.util.ReflectionUtil;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class ReflectionUtilTest {
	class Parent {
		private int i;

		public void setI(int i) {
			this.i = i;
		}

		public int getI() {
			return i;
		}
	}

	class Child extends Parent {
		private int j;
		private boolean a;
		private boolean b;

		public boolean isA() {
			return a;
		}

		public boolean hasB() {
			return b;
		}
		public void setJ(int j) {
			this.j = j;
		}

		public int getJ() {
			return j;
		}
	}

	@Test
	public void shouldFindDeclaredMethods() {
		Method getter = ReflectionUtil.getGetter(Child.class, "j");
		assertThat(getter).isNotNull();

		Method isbool = ReflectionUtil.getGetter(Child.class, "a");
		assertThat(isbool).isNotNull();

		Method hasbool = ReflectionUtil.getGetter(Child.class, "b");
		assertThat(hasbool).isNotNull();

		Method setter = ReflectionUtil.getSetter(Child.class, "j");
		assertThat(setter).isNotNull();
	}

	@Test
	public void shouldFindInheritedMethods() {
		Method getter = ReflectionUtil.getGetter(Child.class, "i");
		assertThat(getter).isNotNull();

		Method setter = ReflectionUtil.getSetter(Child.class, "i");
		assertThat(setter).isNotNull();
	}

	@Test
	public void shouldFindSizeMethod() {
		Method size = ReflectionUtil.getGetter(List.class, "size");
		assertThat(size).isNotNull();
	}
}
