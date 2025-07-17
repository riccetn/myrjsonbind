package se.narstrom.myr.json.bind.serializer.defaults;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;

public record OldProperty(Type type, String name, Field field, Method getter, Method setter) {

	public Object getValue(final Object bean) {
		try {
			if (getter != null) {
				try {
					getter.setAccessible(true);
					return getter.invoke(bean);
				} finally {
					getter.setAccessible(false);
				}
			} else if (field != null) {
				try {
					field.setAccessible(true);
					return field.get(bean);
				} finally {
					field.setAccessible(false);
				}
			} else {
				throw new JsonbException("property is not readable");
			}
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	public void setValue(final Object bean, final Object value) {
		try {
			if (setter != null) {
				try {
					setter.setAccessible(true);
					setter.invoke(bean, value);
				} finally {
					setter.setAccessible(false);
				}
			} else if (field != null) {
				if (Modifier.isFinal(field.getModifiers()))
					return;

				try {
					field.setAccessible(true);
					field.set(bean, value);
				} finally {
					field.setAccessible(false);
				}
			}
			// Non-writeable properties is not an error.
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}
}
