package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.time.Period;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-time
public final class PeriodSerializer implements JsonbSerializer<Period>, JsonbDeserializer<Period> {

	@Override
	public Period deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		return Period.parse(parser.getString());
	}

	@Override
	public void serialize(final Period obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.write(obj.toString());
	}

}
