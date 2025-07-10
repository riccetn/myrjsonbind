package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#enum
public final class EnumSerializer implements JsonbSerializer<Enum<?>>, JsonbDeserializer<Enum<?>> {

	@Override
	public Enum<?> deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Expected a string found " + parser.currentEvent());

		final Class<?> rawType = ReflectionUtilities.getRawType(type);
		return Enum.valueOf((Class) rawType, parser.getString());
	}

	@Override
	public void serialize(final Enum<?> obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.write(obj.name());
	}

}
