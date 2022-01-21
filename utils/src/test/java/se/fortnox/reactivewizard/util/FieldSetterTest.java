package se.fortnox.reactivewizard.util;

import org.junit.Before;
import org.junit.Test;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FieldSetterTest extends AccessorTest {
    private Setter value;
    private Setter longObservable;
    private Setter genericSuperKey;
    private Setter genericSuperValue;
    private Setter superKey;
    private Setter superValue;

    @Before
    public void setUp() {
        value = ReflectionUtil.getSetter(GenericFieldSubclass.class, "value");
        longObservable = ReflectionUtil.getSetter(GenericFieldSubclass.class, "longObservable");
        genericSuperKey = ReflectionUtil.getSetter(GenericFieldSubclass.class, "superKey");
        genericSuperValue = ReflectionUtil.getSetter(GenericFieldSubclass.class, "superValue");
        superKey = ReflectionUtil.getSetter(FieldSubclass.class, "superKey");
        superValue = ReflectionUtil.getSetter(FieldSubclass.class, "superValue");
    }

    @Test
    public void shouldCreateFieldSetters() {
        assertThat(Stream.of(value, longObservable, genericSuperKey, genericSuperValue, superKey, superValue).allMatch(FieldSetter.class::isInstance)).isTrue();
    }

    @Test
    public void shouldSetFinalField() throws Exception {
        GenericFieldSubclass foo = new GenericFieldSubclass(1);
        value.invoke(foo, 9);
        assertThat(foo.value).isEqualTo(9);

        genericSuperKey.invoke(foo, "9");
        assertThat(foo.superKey).isEqualTo("9");

        genericSuperValue.invoke(foo, 9);
        assertThat(foo.superValue).isEqualTo(9);

        FieldSubclass bar = new FieldSubclass(1);
        superKey.invoke(bar, "9");
        assertThat(bar.superKey).isEqualTo("9");

        superValue.invoke(bar, 9);
        assertThat(bar.superValue).isEqualTo(9);
    }

    @Test
    public void shouldSupportSetterLambda() {
        GenericFieldSubclass foo = new GenericFieldSubclass(1);
        BiConsumer<GenericFieldSubclass, Integer> setter = value.setterFunction();
        setter.accept(foo, 9);
        assertThat(foo.value).isEqualTo(9);
    }

    @Test
    public void shouldGetParameterType() {
        assertThat(value.getParameterType()).isEqualTo(Integer.class);
        assertThat(genericSuperKey.getParameterType()).isEqualTo(String.class);
        assertThat(genericSuperValue.getParameterType()).isEqualTo(Integer.class);
        assertThat(superKey.getParameterType()).isEqualTo(String.class);
        assertThat(superValue.getParameterType()).isEqualTo(Integer.class);
    }

    @Test
    public void shouldGetGenericParameterType() {
        assertThat(longObservable.getGenericParameterType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
        assertThat(genericSuperKey.getGenericParameterType().toString()).isEqualTo("class java.lang.String");
        assertThat(genericSuperValue.getGenericParameterType().toString()).isEqualTo("class java.lang.Integer");
        assertThat(superKey.getGenericParameterType().toString()).isEqualTo("class java.lang.String");
        assertThat(superValue.getGenericParameterType().toString()).isEqualTo("class java.lang.Integer");
    }
}
