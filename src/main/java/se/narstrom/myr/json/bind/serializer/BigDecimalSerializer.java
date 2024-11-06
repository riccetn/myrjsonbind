package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.math.BigDecimal;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class BigDecimalSerializer implements JsonbSerializer<BigDecimal>, JsonbDeserializer<BigDecimal> {

	@Override
	public BigDecimal deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (parser.currentEvent() != Event.VALUE_NUMBER)
			throw new JsonbException("Expected a number found " + parser.currentEvent());
		return parser.getBigDecimal();
	}

	@Override
	public void serialize(final BigDecimal number, final JsonGenerator generator, final SerializationContext context) {
		generator.write(number);
	}

}
