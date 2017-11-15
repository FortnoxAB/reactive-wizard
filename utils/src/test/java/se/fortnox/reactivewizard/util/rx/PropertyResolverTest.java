package se.fortnox.reactivewizard.util.rx;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class PropertyResolverTest {
	public static class Mutable {
		private int a;

		public int getA() {
			return a;
		}

		public void setA(int a) {
			this.a = a;
		}
	}

	public static class Immutable {
		private final int a;

		public Immutable(int a) {
			this.a = a;
		}

		public int getA() {
			return a;
		}
	}

	@Test
	public void shouldGetAndSetPropertiesMutable() throws Exception {
		PropertyResolver a = PropertyResolver.from(Mutable.class, new String[] {"a"}).orElse(null);
		assertThat(a).isNotNull();

		Mutable mutable = new Mutable();
		mutable.setA(5);
		assertThat(a.getValue(mutable)).isEqualTo(5);
		a.setValue(mutable, 10);
		assertThat(a.getValue(mutable)).isEqualTo(10);
	}

	@Test
	public void shouldGetAndSetPropertiesImmutable() throws Exception {
		PropertyResolver a = PropertyResolver.from(Immutable.class, new String[] {"a"}).orElse(null);
		assertThat(a).isNotNull();

		Immutable immutable = new Immutable(5);
		assertThat(a.getValue(immutable)).isEqualTo(5);
		a.setValue(immutable, 10);
		assertThat(a.getValue(immutable)).isEqualTo(10);
	}
}