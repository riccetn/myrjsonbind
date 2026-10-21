package se.narstrom.myr.json.bind.serializer.defaults.serializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;

public record Property(Type beanType, Type propertyType, String name, Field field, Method getter) {

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
}
