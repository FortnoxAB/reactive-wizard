package se.fortnox.reactivewizard.json;

import java.util.Objects;

public class ImmutableEntity {
    private final String stringProperty;
    private final int    intProperty;

    public ImmutableEntity(String stringProperty, int intProperty) {
        this.stringProperty = stringProperty;
        this.intProperty = intProperty;
    }

    public String getStringProperty() {
        return stringProperty;
    }

    public int getIntProperty() {
        return intProperty;
    }

    @Override
    public String toString() {
        return "ImmutableEntity{" + "stringProperty='" + stringProperty + '\'' + ", intProperty=" + intProperty + '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        ImmutableEntity that = (ImmutableEntity)object;
        return intProperty == that.intProperty && Objects.equals(stringProperty, that.stringProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringProperty, intProperty);
    }
}
