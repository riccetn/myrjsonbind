package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;

import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-lang-boolean
public final class BooleanSerializer implements JsonbSerializer<Boolean>, JsonbDeserializer<Boolean> {

	@Override
	public Boolean deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
		return switch (parser.currentEvent()) {
			case VALUE_TRUE -> Boolean.TRUE;
			case VALUE_FALSE -> Boolean.FALSE;
			default -> throw new JsonbException("Not a boolean");
		};
	}

	@Override
	public void serialize(final Boolean obj, final JsonGenerator generator, final SerializationContext ctx) {
		if (obj == Boolean.TRUE)
			generator.write(JsonValue.TRUE);
		else if (obj == Boolean.FALSE)
			generator.write(JsonValue.FALSE);
		else
			throw new AssertionError("Not reachable");
	}

}
