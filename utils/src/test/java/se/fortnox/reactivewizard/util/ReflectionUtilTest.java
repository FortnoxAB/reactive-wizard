package se.fortnox.reactivewizard.util;

import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ReflectionUtilTest {
    @Test
    public void shouldFindDeclaredMethods() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "j");
        assertThat(getter).isNotNull();

        Getter isbool = ReflectionUtil.getGetter(Child.class, "a");
        assertThat(isbool).isNotNull();

        Getter hasbool = ReflectionUtil.getGetter(Child.class, "b");
        assertThat(hasbool).isNotNull();

        Setter setter = ReflectionUtil.getSetter(Child.class, "j");
        assertThat(setter).isNotNull();
    }

    @Test
    public void shouldFindSuperclassMethods() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "i");
        assertThat(getter).isNotNull();
        assertThat(getter).isInstanceOf(MethodGetter.class);

        getter = ReflectionUtil.getGetter(Child.class, "k");
        assertThat(getter).isNotNull();
        assertThat(getter).isInstanceOf(MethodGetter.class);

        Setter setter = ReflectionUtil.getSetter(Child.class, "i");
        assertThat(setter).isNotNull();
        assertThat(setter).isInstanceOf(MethodSetter.class);
    }

    @Test
    public void shouldFindSuperclassFields() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "l");
        assertThat(getter).isNotNull();
        assertThat(getter).isInstanceOf(FieldGetter.class);

        Setter setter = ReflectionUtil.getSetter(Child.class, "l");
        assertThat(setter).isNotNull();
        assertThat(setter).isInstanceOf(FieldSetter.class);
    }

    @Test
    public void shouldFindSizeMethod() {
        Getter size = ReflectionUtil.getGetter(List.class, "size");
        assertThat(size).isNotNull();
    }

    @Test
    public void shouldInstantiate() {
        assertThat(ReflectionUtil.newInstance(Parent.class)).isNotNull();
        assertThat(ReflectionUtil.newInstance(Child.class)).isNotNull();
        assertThat(ReflectionUtil.newInstance(PrivateDefaultConstructor.class)).isNotNull();
    }

    @Test
    public void shouldCreateInstantiator() {
        assertThat(ReflectionUtil.instantiator(Parent.class).get()).isInstanceOf(Parent.class);
    }


    @Test
    public void shouldThrowHelpfulExceptionWhenNoZeroParametersConstructorExists()  {
        try {
            ReflectionUtil.newInstance(NoZeroParametersConstructorClass.class);
            fail("Expected RuntimeException, but none was thrown");
        } catch (RuntimeException exception) {
            assertThat(exception.getMessage())
                .isEqualTo("No constructor with zero parameters found on NoZeroParametersConstructorClass");
        }
    }

    @Test
    public void shouldFindFieldIfNoMethod() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "c");
        assertThat(getter).isNotNull();

        Setter setter = ReflectionUtil.getSetter(Child.class, "c");
        assertThat(setter).isNotNull();
    }

    static class Parent {
        private int i;
        private int k;
        private int l;

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        protected int getK() {
            return k;
        }
    }

    class NoZeroParametersConstructorClass {
        NoZeroParametersConstructorClass(String something) {

        }
    }

    static class Child extends Parent {
        private int     j;
        private boolean a;
        private boolean b;
        private boolean c;

        public boolean isA() {
            return a;
        }

        public boolean hasB() {
            return b;
        }

        public int getJ() {
            return j;
        }

        public void setJ(int j) {
            this.j = j;
        }
    }

    static class PrivateDefaultConstructor {
        private final int a;

        private PrivateDefaultConstructor() {
            a = 0;
        }

        public PrivateDefaultConstructor(int a) {
            this.a = a;
        }
    }
}
