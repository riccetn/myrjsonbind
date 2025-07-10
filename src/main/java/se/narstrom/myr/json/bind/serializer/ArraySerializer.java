package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public final class ArraySerializer implements JsonbSerializer<Object>, JsonbDeserializer<Object> {

	@Override
	public void serialize(final Object object, final JsonGenerator generator, final SerializationContext context) {
		if (!object.getClass().isArray())
			throw new JsonbException("Expected an array, got object of type: " + object.getClass().getTypeName());

		generator.writeStartArray();

		for (int i = 0; i < Array.getLength(object); ++i) {
			final Object elem = Array.get(object, i);
			context.serialize(elem, generator);
		}

		generator.writeEnd();
	}

	@Override
	public Object deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (!ReflectionUtilities.getRawType(type).isArray())
			throw new JsonbException("Expected an array, got type: " + type.getTypeName());

		final Type componentType = ReflectionUtilities.getComponentType(type);
		final List<Object> elements = new ArrayList<>();

		while (parser.next() != Event.END_ARRAY) {
			final Object elem = context.deserialize(componentType, parser);
			elements.add(elem);
		}

		// Calling List.toArray() dose not work for primitive component types
		final Object array = Array.newInstance(ReflectionUtilities.getRawType(componentType), elements.size());

		int index = 0;
		for (final Object element : elements) {
			Array.set(array, index++, element);
		}

		return array;
	}
}
