package se.fortnox.reactivewizard;

import org.junit.Test;
import se.fortnox.reactivewizard.util.Getter;
import se.fortnox.reactivewizard.util.ReflectionUtil;
import se.fortnox.reactivewizard.util.Setter;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

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
    public void shouldFindInheritedMethods() {
        Getter getter = ReflectionUtil.getGetter(Child.class, "i");
        assertThat(getter).isNotNull();

        Setter setter = ReflectionUtil.getSetter(Child.class, "i");
        assertThat(setter).isNotNull();
    }

    @Test
    public void shouldFindSizeMethod() {
        Getter size = ReflectionUtil.getGetter(List.class, "size");
        assertThat(size).isNotNull();
    }

    @Test
    public void shouldInstantiate() throws Exception {
        assertThat(ReflectionUtil.newInstance(Parent.class)).isNotNull();
        assertThat(ReflectionUtil.newInstance(Child.class)).isNotNull();
        assertThat(ReflectionUtil.newInstance(PrivateDefaultConstructor.class)).isNotNull();
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

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
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
