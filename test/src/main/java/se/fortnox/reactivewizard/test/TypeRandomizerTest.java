package se.fortnox.reactivewizard.test;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.fest.assertions.Assertions.assertThat;

public class TypeRandomizerTest {
    enum TestEnum {
        TEST1, TEST2
    }

    @Test(expected = RuntimeException.class)
    public void testTypeRandomizerThrowsExceptionWhenClassCanNotBeInstantiated() {
        TypeRandomizer.getType(AbstractPojo.class);
    }

    @Test
    public void testTypeRandomizer() throws InvocationTargetException, IllegalAccessException {
        TestPojo testPojo = TypeRandomizer.getType(TestPojo.class);

        for (Method declaredMethod : TestPojo.class.getDeclaredMethods()) {
            assertThat(declaredMethod.invoke(testPojo)).isNotNull();
        }

        for (Method declaredMethod : TestPojo.class.getSuperclass().getDeclaredMethods()) {
            assertThat(declaredMethod.invoke(testPojo)).isNotNull();
        }
    }
}

