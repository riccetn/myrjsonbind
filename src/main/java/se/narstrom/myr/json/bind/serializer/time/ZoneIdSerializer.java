package se.narstrom.myr.json.bind.serializer.time;

import java.lang.reflect.Type;
import java.time.ZoneId;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-time
public final class ZoneIdSerializer implements JsonbSerializer<ZoneId>, JsonbDeserializer<ZoneId> {

	@Override
	public ZoneId deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		return ZoneId.of(parser.getString());
	}

	@Override
	public void serialize(final ZoneId obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.write(obj.getId());
	}
}
