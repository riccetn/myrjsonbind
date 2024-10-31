package se.narstrom.myr.json.bind;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Objects;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class DefaultDeserializer implements JsonbDeserializer<Object> {

	@Override
	public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		final Class<?> clazz = (Class<?>) type;

		assert !clazz.isArray() && !clazz.isPrimitive();

		if (parser.currentEvent() != Event.START_OBJECT)
			throw new JsonbException("Not an object");

		return deserializeObject(parser, ctx, clazz);
	}

	private Object deserializeObject(final JsonParser parser, final DeserializationContext ctx, final Class<?> clazz) {

		final Constructor<?> constructor = findConstructor(clazz);
		if (constructor == null)
			throw new JsonbException("No public or protected no-args constructor");

		final Object instance;
		try {
			constructor.setAccessible(true);
			instance = constructor.newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		} finally {
			constructor.setAccessible(false);
		}

		Event event;
		while ((event = parser.next()) != Event.END_OBJECT) {
			assert event == Event.KEY_NAME;
			final String name = parser.getString();

			if (name.isEmpty()) {
				skipObject(parser);
				continue;
			}

			Field field;
			try {
				field = clazz.getDeclaredField(name);
			} catch (final NoSuchFieldException ex) {
				field = null;
			}

			if (field != null) {
				if (Modifier.isStatic(field.getModifiers())) {
					skipObject(parser);
					continue;
				}

				if (Modifier.isFinal(field.getModifiers())) {
					skipObject(parser);
					continue;
				}

				if (Modifier.isTransient(field.getModifiers())) {
					skipObject(parser);
					continue;
				}
			}

			Type propertyType = null;
			Class<?> propertyClazz = null;
			if (field != null) {
				propertyType = field.getGenericType();
				propertyClazz = field.getType();
			}

			final Method setter;
			if (propertyType != null) {
				setter = getSetter(clazz, name, propertyClazz);

				if (setter != null) {
					if (!Modifier.isPublic(setter.getModifiers())) {
						skipObject(parser);
						continue;
					}

					if (Modifier.isStatic(setter.getModifiers())) {
						skipObject(parser);
						continue;
					}

					if (setter.isBridge()) {
						skipObject(parser);
						continue;
					}
				}
			} else {
				setter = findSetter(clazz, name);

				if (setter != null) {
					propertyType = setter.getGenericParameterTypes()[0];
					propertyClazz = setter.getParameterTypes()[0];
				}
			}

			if (propertyType == null) {
				skipObject(parser);
				continue;
			}

			if (setter == null && !Modifier.isPublic(field.getModifiers())) {
				skipObject(parser);
				continue;
			}

			final Object value = ctx.deserialize(propertyType, parser);

			if (setter != null) {
				try {
					setter.invoke(instance, value);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				}
			} else {
				assert field != null;

				try {
					field.setAccessible(true);
					field.set(instance, value);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				} finally {
					field.setAccessible(false);
				}
			}
		}

		return instance;
	}

	private void skipObject(final JsonParser parser) {
		parser.getValue();
		parser.next();
	}

	private Constructor<?> findConstructor(final Class<?> clazz) {
		for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {

			if (!Modifier.isPublic(constructor.getModifiers()) && !Modifier.isProtected(constructor.getModifiers()))
				continue;

			if (constructor.getParameterCount() != 0)
				continue;

			return constructor;
		}
		return null;
	}

	private Method getSetter(final Class<?> clazz, final String name, final Class<?> propertyClazz) {
		final int firstCodePoint = name.codePointAt(0);
		final String setterName;

		if (firstCodePoint > 0xFFFF)
			setterName = "set" + Character.toString(Character.toUpperCase(firstCodePoint)) + name.substring(2);
		else
			setterName = "set" + Character.toString(Character.toUpperCase(firstCodePoint)) + name.substring(1);

		try {
			return clazz.getDeclaredMethod(setterName, propertyClazz);
		} catch (final NoSuchMethodException ex) {
			return null;
		}
	}

	private Method findSetter(final Class<?> clazz, final String name) {
		final int firstCodePoint = name.codePointAt(0);
		final String setterName;

		if (firstCodePoint > 0xFFFF)
			setterName = "set" + Character.toString(Character.toUpperCase(firstCodePoint)) + name.substring(2);
		else
			setterName = "set" + Character.toString(Character.toUpperCase(firstCodePoint)) + name.substring(1);

		for (Class<?> currentClazz = clazz; currentClazz != null && currentClazz != Object.class; currentClazz = currentClazz.getSuperclass()) {
			for (final Method method : currentClazz.getMethods()) {

				if (!Modifier.isPublic(method.getModifiers()))
					continue;

				if (Modifier.isStatic(method.getModifiers()))
					continue;

				if (method.isBridge())
					continue;

				if (method.getParameterCount() != 1)
					continue;

				if (!Objects.equals(method.getName(), setterName))
					continue;

				return method;
			}
		}

		return null;
	}
}
