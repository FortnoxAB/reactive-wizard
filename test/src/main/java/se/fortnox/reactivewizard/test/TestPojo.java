package se.fortnox.reactivewizard.test;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class TestPojo extends PrimitivePojo {
    private TypeRandomizerTest.TestEnum testEnum;
    private Float floatClass;
    private Integer integerClass;
    private Long longClass;
    private Double doubleClass;
    private Boolean booleanClass;

    private String         string;
    private UUID           uuid;
    private BigInteger     bigInteger;
    private LocalDateTime  localDateTime;
    private LocalDate      localDate;
    private OffsetDateTime offsetDateTime;
    private List           list;
    private Map            map;
    private Set            set;

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
