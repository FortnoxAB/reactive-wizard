package se.fortnox.reactivewizard.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MethodSetterTest extends AccessorTest {
    private Setter value;
    private Setter longObservable;
    private Setter genericSuperKey;
    private Setter genericSuperValue;
    private Setter superKey;
    private Setter superValue;

    public void initMethodSetterTest(boolean useLambdas) {
        LambdaCompiler.useLambdas = useLambdas;
        value = ReflectionUtil.getSetter(GenericMethodSubclass.class, "value");
        longObservable = ReflectionUtil.getSetter(GenericMethodSubclass.class, "longObservable");
        genericSuperKey = ReflectionUtil.getSetter(GenericMethodSubclass.class, "superKey");
        genericSuperValue = ReflectionUtil.getSetter(GenericMethodSubclass.class, "superValue");
        superKey = ReflectionUtil.getSetter(MethodSubclass.class, "superKey");
        superValue = ReflectionUtil.getSetter(MethodSubclass.class, "superValue");
    }

    public static Collection useLambdasParameters() {
        return List.of(new Object[][] {{ true }, { false }});
    }

    @MethodSource("useLambdasParameters")
    @ParameterizedTest
    void shouldCreateMethodSetters(boolean useLambdas) {
        initMethodSetterTest(useLambdas);
        assertThat(Stream.of(value, longObservable, genericSuperKey, genericSuperValue, superKey, superValue).allMatch(MethodSetter.class::isInstance)).isTrue();
    }

    @MethodSource("useLambdasParameters")
    @ParameterizedTest
    void shouldSetValue(boolean useLambdas) throws Exception {
        initMethodSetterTest(useLambdas);
        GenericMethodSubclass foo = new GenericMethodSubclass(1);
        value.invoke(foo, 9);
        assertThat(foo.getValue()).isEqualTo(9);

        genericSuperKey.invoke(foo, "9");
        assertThat(foo.getSuperKey()).isEqualTo("9");

        genericSuperValue.invoke(foo, 9);
        assertThat(foo.getSuperValue());

        MethodSubclass bar = new MethodSubclass("1", 2);
        superKey.invoke(bar, "9");
        assertThat(bar.getSuperKey()).isEqualTo("9");

        superValue.invoke(bar, 9);
        assertThat(bar.getSuperValue()).isEqualTo(9);
    }

    @MethodSource("useLambdasParameters")
    @ParameterizedTest
    void shouldGetParameterType(boolean useLambdas) {
        initMethodSetterTest(useLambdas);
        assertThat(value.getParameterType()).isEqualTo(Integer.class);
        assertThat(genericSuperKey.getParameterType()).isEqualTo(String.class);
        assertThat(genericSuperValue.getParameterType()).isEqualTo(Integer.class);
        assertThat(superKey.getParameterType()).isEqualTo(String.class);
        assertThat(superValue.getParameterType()).isEqualTo(Integer.class);
    }

    @MethodSource("useLambdasParameters")
    @ParameterizedTest
    void shouldGetGenericParameterType(boolean useLambdas) {
        initMethodSetterTest(useLambdas);
        assertThat(longObservable.getGenericParameterType().toString()).isEqualTo("rx.Observable<java.lang.Long>");
        assertThat(genericSuperKey.getGenericParameterType().toString()).isEqualTo("class java.lang.String");
        assertThat(genericSuperValue.getGenericParameterType().toString()).isEqualTo("class java.lang.Integer");
        assertThat(superKey.getGenericParameterType().toString()).isEqualTo("class java.lang.String");
        assertThat(superValue.getGenericParameterType().toString()).isEqualTo("class java.lang.Integer");
    }
}
