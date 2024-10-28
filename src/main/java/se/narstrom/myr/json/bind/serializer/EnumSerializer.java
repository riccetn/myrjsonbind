package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#enum
public final class EnumSerializer implements JsonbSerializer<Enum<?>>, JsonbDeserializer<Enum<?>> {

	@Override
	public Enum<?> deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");

		final Class<?> clazz = (Class<?>) type;

		try {
			final Method method = clazz.getMethod("valueOf", String.class);
			assert Modifier.isStatic(method.getModifiers());

			return (Enum<?>) method.invoke(null, parser.getString());
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final Enum<?> obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.write(obj.name());
	}

}
