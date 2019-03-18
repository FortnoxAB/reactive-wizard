package se.fortnox.reactivewizard.json;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;

public class Types {
	public static <T> TypeReference<T> toReference(Type type) {
		return new TypeReference<T>() {
			@Override
			public Type getType() {
				return type;
			}
		};
	}
}
