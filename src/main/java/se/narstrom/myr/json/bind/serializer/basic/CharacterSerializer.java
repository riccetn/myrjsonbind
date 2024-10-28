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

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-lang-string-character
public final class CharacterSerializer implements JsonbSerializer<Character>, JsonbDeserializer<Character> {

	@Override
	public Character deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		final String value = parser.getString();
		if (value.length() != 1)
			throw new JsonbException("Length of string not 1");
		return value.charAt(0);
	}

	@Override
	public void serialize(final Character obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.write(obj.toString());
	}
}
