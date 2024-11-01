package se.narstrom.myr.json.bind;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import jakarta.json.bind.JsonbException;

public final class ReflectionUilities {
	private ReflectionUilities() {
	}

	public static Class<?> getClass(final Type type) {
		if (type instanceof Class<?> clazz)
			return clazz;
		else if (type instanceof ParameterizedType parameterized)
			return getClass(parameterized.getRawType());
		else if (type instanceof TypeVariable<?> variable) {
			final Type[] upperBounds = variable.getBounds();
			if (upperBounds.length != 0)
				return Object.class; // TODO: Find the nearest common super of all types in upperBounds
			return getClass(upperBounds[0]);
		} else
			throw new JsonbException("Unsupported type " + type);
	}

	public static Type getComponentType(final Type type) {
		if (type instanceof GenericArrayType genericType) {
			return genericType.getGenericComponentType();
		} else if (type instanceof Class<?> clazz) {
			return clazz.getComponentType();
		} else {
			throw new JsonbException("Unsupported array type " + type);
		}
	}
}
