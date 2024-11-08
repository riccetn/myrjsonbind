package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.MyrJsonbContext;
import se.narstrom.myr.json.bind.reflect.Properties;
import se.narstrom.myr.json.bind.reflect.Property;
import se.narstrom.myr.json.bind.reflect.ReflectionUilities;

public final class DefaultDeserializer implements JsonbDeserializer<Object> {

	@Override
	public Object deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (parser.currentEvent() == Event.VALUE_NULL)
			return null;

		if (parser.currentEvent() != Event.START_OBJECT)
			throw new JsonbException("Not an object, event: " + parser.currentEvent());

		return deserializeObject(parser, context, type);
	}

	private Object deserializeObject(final JsonParser parser, final DeserializationContext context, final Type type) {
		final Class<?> rawType = ReflectionUilities.getRawType(type);

		assert !rawType.isArray() && !rawType.isPrimitive();

		final Constructor<?> constructor = findConstructor(rawType);
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

		final Map<String, Property> properties = Properties.getProperties(type);

		Event event;
		while ((event = parser.next()) != Event.END_OBJECT) {
			assert event == Event.KEY_NAME;
			final String name = parser.getString();

			final Property property = properties.get(name);

			if (property == null) {
				skipObject(parser, context);
				continue;
			}

			final Object value = context.deserialize(property.type(), parser);
			property.setValue(instance, value);
		}

		return instance;
	}

	private void skipObject(final JsonParser parser, final DeserializationContext context) {
		if (context instanceof MyrJsonbContext myrContext && myrContext.getConfig().getProperty("jsonb.fail-on-unknown-properties").orElse(Boolean.FALSE) == Boolean.TRUE)
			throw new JsonbException("No java property for json property");
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
}
