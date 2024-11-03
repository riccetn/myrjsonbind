package se.narstrom.myr.json.bind.serializer.optional;

import java.lang.reflect.Type;
import java.util.OptionalDouble;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class OptionalDoubleSerializer implements JsonbSerializer<OptionalDouble>, JsonbDeserializer<OptionalDouble> {

	@Override
	public OptionalDouble deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (parser.currentEvent() == Event.VALUE_NULL)
			return OptionalDouble.empty();
		if (parser.currentEvent() != Event.VALUE_NUMBER)
			throw new JsonbException("Not a number");
		return OptionalDouble.of(parser.getBigDecimal().doubleValue());
	}

	@Override
	public void serialize(final OptionalDouble optional, final JsonGenerator generator, final SerializationContext context) {
		if (optional.isEmpty())
			generator.writeNull();
		else
			generator.write(optional.getAsDouble());
	}

}
