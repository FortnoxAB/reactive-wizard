package se.fortnox.reactivewizard.test;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

abstract class AbstractPojo {

}

class PrimitivePojo {
    private float floatPrimitive;
    private int intPrimitive;
    private long longPrimitive;
    private double doublePrimitive;
    private boolean booleanPrimitive;

    public float getFloatPrimitive() {
        return floatPrimitive;
    }

    public int getIntPrimitive() {
        return intPrimitive;
    }

    public long getLongPrimitive() {
        return longPrimitive;
    }

    public double getDoublePrimitive() {
        return doublePrimitive;
    }

    public boolean isBooleanPrimitive() {
        return booleanPrimitive;
    }
}

class TestPojo extends PrimitivePojo {
    private TypeRandomizerTest.TestEnum testEnum;
    private Float floatClass;
    private Integer integerClass;
    private Long longClass;
    private Double doubleClass;
    private Boolean booleanClass;

    private String string;
    private UUID uuid;
    private BigInteger bigInteger;
    private LocalDateTime localDateTime;
    private LocalDate localDate;
    private OffsetDateTime offsetDateTime;
    private List list;
    private Map map;
    private Set set;

    public TypeRandomizerTest.TestEnum getTestEnum() {
        return testEnum;
    }

    public Float getFloatClass() {
        return floatClass;
    }

    public Integer getIntegerClass() {
        return integerClass;
    }

    public Long getLongClass() {
        return longClass;
    }

    public Double getDoubleClass() {
        return doubleClass;
    }

    public Boolean getBooleanClass() {
        return booleanClass;
    }

    public String getString() {
        return string;
    }

    public UUID getUuid() {
        return uuid;
    }

    public BigInteger getBigInteger() {
        return bigInteger;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public List getList() {
        return list;
    }

    public Map getMap() {
        return map;
    }

    public Set getSet() {
        return set;
    }
}
