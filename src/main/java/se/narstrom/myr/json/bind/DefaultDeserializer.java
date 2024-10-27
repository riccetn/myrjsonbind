package se.narstrom.myr.json.bind;

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
		System.out.println("Deserialize an instance of " + type.toString());

		if (parser.next() != Event.START_OBJECT)
			throw new JsonbException("Not an object");

		final Class<?> clazz = (Class<?>) type;
		final Object instance;
		try {
			instance = clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}

		Event event;
		while ((event = parser.next()) != Event.END_OBJECT) {
			assert event == Event.KEY_NAME;
			final String key = parser.getString();

			if (key.isEmpty())
				continue;

			final Method setter = findSetter(clazz, key);
			if (setter != null) {
				final Class<?> propertyType = setter.getParameterTypes()[0];
				final Object value = ctx.deserialize(propertyType, parser);
				try {
					setter.invoke(instance, value);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				}
			} else {
				Field field;
				try {
					field = clazz.getField(key);
				} catch (final NoSuchFieldException ex) {
					field = null;
				}

				if (field != null) {
					final Class<?> propertyType = field.getType();
					final Object value = ctx.deserialize(propertyType, parser);
					try {
						field.set(propertyType, value);
					} catch (final ReflectiveOperationException ex) {
						throw new JsonbException(ex.getMessage(), ex);
					}
				} else {
					parser.getValue();
					parser.next();
				}
			}
		}

		return instance;
	}

	private Method findSetter(final Class<?> clazz, final String key) {
		final int firstCodePoint = key.codePointAt(0);
		if (!Character.isJavaIdentifierStart(firstCodePoint))
			return null;

		final String setterName = "set" + Character.toString(Character.toUpperCase(firstCodePoint)) + key.substring(1);

		for (Class<?> currentClazz = clazz; currentClazz != null && currentClazz != Object.class; currentClazz = currentClazz.getSuperclass()) {
			for (final Method method : currentClazz.getMethods()) {

				if (!Modifier.isPublic(method.getModifiers()))
					continue;

				if (method.isBridge())
					continue;

				if (!Objects.equals(method.getName(), setterName))
					continue;

				if (method.getParameterCount() != 1)
					continue;

				return method;
			}
		}

		return null;
	}
}
