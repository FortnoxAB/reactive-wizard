package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;

public interface TypeHolder {
	Type getType();

	static TypeHolder fromType(Type type) {
		return () -> type;
	}

}
