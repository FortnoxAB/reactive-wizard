package se.fortnox.reactivewizard.json;

import java.util.Objects;

public class MutableEntity {
	private String stringProperty;
	private int intProperty;

	public String getStringProperty() {
		return stringProperty;
	}

	public void setStringProperty(String stringProperty) {
		this.stringProperty = stringProperty;
	}

	public int getIntProperty() {
		return intProperty;
	}

	public void setIntProperty(int intProperty) {
		this.intProperty = intProperty;
	}

	@Override
	public String toString() {
		return "MutableEntity{" + "stringProperty='" + stringProperty + '\'' + ", intProperty=" + intProperty + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MutableEntity that = (MutableEntity) o;
		return intProperty == that.intProperty && Objects.equals(stringProperty, that.stringProperty);
	}

	@Override
	public int hashCode() {
		return Objects.hash(stringProperty, intProperty);
	}
}
