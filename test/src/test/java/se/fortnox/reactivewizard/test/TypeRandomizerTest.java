package se.fortnox.reactivewizard.test;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.fest.assertions.Assertions.assertThat;

public class TypeRandomizerTest {

    @Test(expected = RuntimeException.class)
    public void testTypeRandomizerThrowsExceptionWhenClassCanNotBeInstantiated() {
        TypeRandomizer.getType(AbstractPojo.class);
    }

    @Test
    public void testTypeRandomizer() throws InvocationTargetException, IllegalAccessException {
        TestPojo testPojo = TypeRandomizer.getType(TestPojo.class);

        for (Method declaredMethod : TestPojo.class.getDeclaredMethods()) {
            if (declaredMethod.getName().startsWith("$")) {
                continue;
            }
            assertThat(declaredMethod.invoke(testPojo)).isNotNull();
        }

        for (Method declaredMethod : TestPojo.class.getSuperclass().getDeclaredMethods()) {
            if (declaredMethod.getName().startsWith("$")) {
                continue;
            }
            assertThat(declaredMethod.invoke(testPojo)).isNotNull();
        }
    }
}

