package se.fortnox.reactivewizard.util;

import org.junit.Test;
import se.fortnox.reactivewizard.util.PropertyResolver;

import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class PropertyResolverTest {
    @Test
    public void shouldGetAndSetPropertiesMutable() throws Exception {
        PropertyResolver propertyResolver = PropertyResolver.from(Mutable.class, new String[]{"anInt"}).orElse(null);
        assertThat(propertyResolver).isNotNull();

        Mutable mutable = new Mutable();
        mutable.setAnInt(5);
        assertThat(propertyResolver.getter().apply(mutable)).isEqualTo(5);
        propertyResolver.setter().accept(mutable, 10);
        assertThat(propertyResolver.getter().apply(mutable)).isEqualTo(10);
    }

    @Test
    public void shouldGetAndSetPropertiesImmutable() throws Exception {
        PropertyResolver propertyResolver = PropertyResolver.from(Immutable.class, new String[]{"anInt"}).orElse(null);
        assertThat(propertyResolver).isNotNull();

        Immutable immutable = new Immutable(5);
        assertThat(propertyResolver.getter().apply(immutable)).isEqualTo(5);
        propertyResolver.setter().accept(immutable, 10);
        assertThat(propertyResolver.getter().apply(immutable)).isEqualTo(10);
    }

    @Test
    public void emptyPropertyResolver() {
        try {
            PropertyResolver.from(Mutable.class, new String[0]).get().setter();
            fail("expected exception");
        } catch (IllegalArgumentException e) {}

        Mutable instance = new Mutable();
        Function getter = PropertyResolver.from(Mutable.class, new String[0]).get().getter();
        assertThat(getter.apply(instance)).isSameAs(instance);
    }

    @Test
    public void missingProperty() {
        assertThat(PropertyResolver.from(Mutable.class, new String[]{"nonexisting"}).isPresent()).isFalse();

        PropertyResolver propertyResolver = PropertyResolver.from(Mutable.class, new String[0]).get();
        assertThat(propertyResolver.subPath(new String[]{"nonexisting"}).isPresent()).isFalse();
    }

    @Test
    public void testToString() {
        PropertyResolver.from(Mutable.class, new String[]{"anInt"}).get().toString();
    }

    public static class Mutable {
        private int anInt;

        public int getAnInt() {
            return anInt;
        }

        public void setAnInt(int anInt) {
            this.anInt = anInt;
        }
    }

    public static class Immutable {
        private final int anInt;

        public Immutable(int anInt) {
            this.anInt = anInt;
        }

        public int getAnInt() {
            return anInt;
        }
    }
}
