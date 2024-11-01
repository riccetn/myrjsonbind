package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.util.Map;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

public final class MapSerializer implements JsonbSerializer<Map<String, ?>>, JsonbDeserializer<Map<String, ?>> {

	@Override
	public Map<String, ?> deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void serialize(final Map<String, ?> map, final JsonGenerator generator, final SerializationContext context) {
		generator.writeStartObject();
		for (final Map.Entry<String, ?> entry : map.entrySet()) {
			context.serialize(entry.getKey(), entry.getValue(), generator);
		}
		generator.writeEnd();
	}

}
