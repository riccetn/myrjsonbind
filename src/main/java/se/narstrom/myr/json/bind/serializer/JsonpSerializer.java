package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;

import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

public final class JsonpSerializer implements JsonbSerializer<JsonValue>, JsonbDeserializer<JsonValue> {

	@Override
	public JsonValue deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		return parser.getValue();
	}

	@Override
	public void serialize(final JsonValue object, final JsonGenerator generator, final SerializationContext context) {
		generator.write(object);
	}

}
