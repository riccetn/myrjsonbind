package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.math.BigInteger;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

public final class BigIntegerSerializer implements JsonbSerializer<BigInteger>, JsonbDeserializer<BigInteger> {

	@Override
	public BigInteger deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		return parser.getBigDecimal().toBigInteger();
	}

	@Override
	public void serialize(final BigInteger number, final JsonGenerator generator, final SerializationContext context) {
		generator.write(number);
	}

}
