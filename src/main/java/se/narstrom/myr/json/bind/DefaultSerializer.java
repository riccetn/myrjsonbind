package se.narstrom.myr.json.bind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

public final class DefaultSerializer implements JsonbSerializer<Object> {

	@Override
	public void serialize(final Object object, final JsonGenerator generator, final SerializationContext context) {
		final Class<?> clazz = object.getClass();

		assert !clazz.isArray() && !clazz.isPrimitive();

		generator.writeStartObject();
		serializeProperties(object, clazz, generator, context);
		generator.writeEnd();
	}

	private void serializeProperties(final Object object, final Class<?> clazz, final JsonGenerator generator, final SerializationContext context) {
		final Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && superClazz != Object.class)
			serializeProperties(object, superClazz, generator, context);

		final Map<String, Property> properties = new HashMap<>();

		for (final Field field : clazz.getDeclaredFields()) {
			final Property property = new Property();
			property.name = field.getName();
			property.field = field;

			properties.put(field.getName(), property);
		}

		for (final Method method : clazz.getDeclaredMethods()) {

			if (method.getParameterCount() != 0)
				continue;

			if (method.isBridge())
				continue;

			final int methodModifiers = method.getModifiers();

			if (Modifier.isStatic(methodModifiers))
				continue;

			final String methodName = method.getName();

			int firstIndex;
			if (methodName.startsWith("get"))
				firstIndex = 3;
			else if (methodName.startsWith("is"))
				firstIndex = 2;
			else
				continue;

			final int firstCodePoint = methodName.codePointAt(firstIndex);
			if (!Character.isUpperCase(firstCodePoint))
				continue;

			final String propertyName = Character.toString(Character.toLowerCase(firstCodePoint)) + ((firstCodePoint > 0xFFFF) ? methodName.substring(firstIndex + 2) : methodName.substring(firstIndex + 1));

			Property property = properties.get(propertyName);
			if (property == null) {
				property = new Property();
				property.name = propertyName;
				properties.put(propertyName, property);
			}
			property.getter = method;
		}

		final List<Property> sortedProperties = new ArrayList<>(properties.values());
		sortedProperties.sort(Comparator.comparing(p -> p.name));

		for (final Property property : sortedProperties) {
			if (property.field != null) {
				final int fieldModifiers = property.field.getModifiers();
				if (Modifier.isTransient(fieldModifiers) || Modifier.isStatic(fieldModifiers))
					continue;
			}

			final Object value;

			if (property.getter != null) {
				if (!Modifier.isPublic(property.getter.getModifiers()))
					continue;

				try {
					value = property.getter.invoke(object);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				}
			} else {
				if (!Modifier.isPublic(property.field.getModifiers()))
					continue;

				try {
					property.field.setAccessible(true);
					value = property.field.get(object);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				} finally {
					property.field.setAccessible(false);
				}
			}

			context.serialize(property.name, value, generator);
		}
	}

	static class Property {
		String name;
		Field field;
		Method getter;
	}
}
