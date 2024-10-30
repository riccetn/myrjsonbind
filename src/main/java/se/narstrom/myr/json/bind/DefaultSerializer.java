package se.narstrom.myr.json.bind;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

public final class DefaultSerializer implements JsonbSerializer<Object> {

	@Override
	public void serialize(final Object obj, final JsonGenerator generator, final SerializationContext ctx) {
		final Class<?> clazz = obj.getClass();

		if (clazz.isArray()) {
			serializeArray(obj, clazz, generator, ctx);
			return;
		}

		generator.writeStartObject();

		for (final Property property : findProperties(clazz)) {
			generator.writeKey(property.name);

			final Object value;
			if (property.getter() != null) {
				try {
					value = property.getter().invoke(obj);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				}
			} else if (property.field() != null) {
				try {
					value = property.field().get(obj);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				}
			} else {
				throw new AssertionError("Not reachable");
			}
			ctx.serialize(value, generator);
		}

		generator.writeEnd();
	}

	private void serializeArray(final Object array, final Class<?> clazz, final JsonGenerator generator, final SerializationContext ctx) {
		generator.writeStartArray();

		for (int i = 0; i < Array.getLength(array); ++i) {
			final Object elem = Array.get(array, i);
			ctx.serialize(elem, generator);
		}

		generator.writeEnd();
	}

	private List<Property> findProperties(final Class<?> clazz) {
		if (clazz == Object.class)
			return List.of();

		final List<Property> result = new ArrayList<>();

		final Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null && superClazz != Object.class)
			result.addAll(findProperties(superClazz));

		final HashMap<String, Property> localProperties = new HashMap<String, Property>();
		for (final Property prop : findFieldProperties(clazz)) {
			localProperties.put(prop.name(), prop);
		}
		for (final Property prop : findGetterProperties(clazz)) {
			localProperties.put(prop.name(), prop);
		}

		// FIXME: Figure out what to do with properties that exists both in a super class and here
		//        i.e. overridden getters, and shadowed fields.
		final List<Property> list = new ArrayList<>(localProperties.values());
		list.sort(Comparator.comparing(Property::name));
		result.addAll(list);

		return result;
	}

	private List<Property> findGetterProperties(final Class<?> clazz) {
		final List<Property> result = new ArrayList<>();

		for (final Method method : clazz.getDeclaredMethods()) {

			if (!Modifier.isPublic(method.getModifiers()))
				continue;

			if (Modifier.isStatic(method.getModifiers()))
				continue;

			if (method.isBridge())
				continue;

			if (method.getParameterCount() != 0)
				continue;

			final String methodName = method.getName();
			if (methodName.equals("getClass"))
				continue;

			final int nameOffset;
			if (methodName.startsWith("get"))
				nameOffset = 3;
			else if (methodName.startsWith("is"))
				nameOffset = 2;
			else
				continue;

			final int firstCodePoint = methodName.codePointAt(nameOffset);
			if (!Character.isUpperCase(firstCodePoint))
				continue;

			final String propertyName;
			if (firstCodePoint > 0xFFFF)
				propertyName = Character.toString(Character.toLowerCase(firstCodePoint)) + methodName.substring(nameOffset + 2);
			else
				propertyName = Character.toString(Character.toLowerCase(firstCodePoint)) + methodName.substring(nameOffset + 1);

			result.add(new Property(propertyName, method, null));
		}

		return result;
	}

	private List<Property> findFieldProperties(final Class<?> clazz) {
		final List<Property> result = new ArrayList<>();

		for (final Field field : clazz.getDeclaredFields()) {

			if (!Modifier.isPublic(field.getModifiers()))
				continue;

			if (Modifier.isStatic(field.getModifiers()))
				continue;

			final String fieldName = field.getName();

			result.add(new Property(fieldName, null, field));
		}

		return result;
	}

	private record Property(String name, Method getter, Field field) {
	}
}
