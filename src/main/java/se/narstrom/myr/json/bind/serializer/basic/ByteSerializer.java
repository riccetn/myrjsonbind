package se.narstrom.myr.json.bind.serializer.basic;

import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-lang-byte-short-integer-long-float-double
public final class ByteSerializer implements JsonbSerializer<Byte>, JsonbDeserializer<Byte> {

	@Override
	public Byte deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_NUMBER)
			throw new JsonbException("Not a number");
		return (byte) parser.getInt();
	}

	@Override
	public void serialize(final Byte obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.write(obj);
	}
}