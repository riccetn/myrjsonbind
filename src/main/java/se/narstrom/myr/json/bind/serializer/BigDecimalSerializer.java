package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.math.BigDecimal;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

public final class BigDecimalSerializer implements JsonbSerializer<BigDecimal>, JsonbDeserializer<BigDecimal> {

	@Override
	public BigDecimal deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		return parser.getBigDecimal();
	}

	@Override
	public void serialize(final BigDecimal number, final JsonGenerator generator, final SerializationContext context) {
		generator.write(number);
	}

}
