package se.fortnox.reactivewizard.util;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;

import java.util.stream.Stream;

import static org.fest.assertions.Assertions.assertThat;
import static rx.Observable.empty;

public class FieldSetterTest {
    private Setter value;
    private Setter longObservable;
    private Setter superKey;
    private Setter superValue;

    @Before
    public void setUp() {
        value = ReflectionUtil.getSetter(Foo.class, "value");
        longObservable = ReflectionUtil.getSetter(Foo.class, "longObservable");
        superKey = ReflectionUtil.getSetter(Foo.class, "superKey");
        superValue = ReflectionUtil.getSetter(Foo.class, "superValue");
    }

    @Test
    public void shouldCreateFieldSetters() {
        assertThat(Stream.of(value, longObservable, superKey, superValue).allMatch(FieldSetter.class::isInstance)).isTrue();
    }

    @Test
    public void shouldSetFinalField() throws Exception {
        Foo foo = new Foo(1);
        value.invoke(foo, 9);
        assertThat(foo.value).isEqualTo(9);

        superKey.invoke(foo, "9");
        assertThat(foo.getSuperKey()).isEqualTo("9");

        superValue.invoke(foo, 9);
        assertThat(foo.getSuperValue()).isEqualTo(9);
    }

    @Test
    public void shouldGetParameterType() {
        assertThat(value.getParameterType()).isEqualTo(Integer.class);
        assertThat(superKey.getParameterType()).isEqualTo(String.class);
        assertThat(superValue.getParameterType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericParameterType() {
        assertThat(longObservable.getGenericParameterType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
        assertThat(superKey.getGenericParameterType().toString()).isEqualTo("class java.lang.String");
        assertThat(superValue.getGenericParameterType().toString()).isEqualTo("class java.lang.Integer");
    }

    private class Foo extends GenericSuper<String, Integer> {
        private final Integer          value;
        private final Observable<Long> longObservable;

        private Foo(Integer value) {
            super(String.valueOf(value), value);
            this.value = value;
            longObservable = empty();
        }
    }

    private class GenericSuper<K, V> {
        private final K superKey;
        private final V superValue;

        private GenericSuper(K key, V value) {
            this.superKey = key;
            this.superValue = value;
        }

        K getSuperKey() {
            return superKey;
        }

        V getSuperValue() {
            return superValue;
        }
    }
}
