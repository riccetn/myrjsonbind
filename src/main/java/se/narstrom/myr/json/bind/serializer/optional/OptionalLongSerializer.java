package se.narstrom.myr.json.bind.serializer.optional;

import java.lang.reflect.Type;
import java.util.OptionalLong;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class OptionalLongSerializer implements JsonbSerializer<OptionalLong>, JsonbDeserializer<OptionalLong> {

	@Override
	public OptionalLong deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (parser.currentEvent() == Event.VALUE_NULL)
			return OptionalLong.empty();
		if (parser.currentEvent() != Event.VALUE_NUMBER)
			throw new JsonbException("Not a number");
		return OptionalLong.of(parser.getLong());
	}

	@Override
	public void serialize(final OptionalLong optional, final JsonGenerator generator, final SerializationContext context) {
		if (optional.isEmpty())
			generator.writeNull();
		else
			generator.write(optional.getAsLong());
	}

}
