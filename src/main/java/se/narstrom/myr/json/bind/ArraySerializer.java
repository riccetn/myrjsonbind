package se.narstrom.myr.json.bind;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class ArraySerializer implements JsonbSerializer<Object>, JsonbDeserializer<Object> {

	@Override
	public void serialize(final Object object, final JsonGenerator generator, final SerializationContext context) {
		final Class<?> clazz = object.getClass();
		assert clazz.isArray();

		generator.writeStartArray();

		for (int i = 0; i < Array.getLength(object); ++i) {
			final Object elem = Array.get(object, i);
			context.serialize(elem, generator);
		}

		generator.writeEnd();
	}

	@Override
	public Object deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		final Class<?> clazz = (Class<?>) type;
		assert clazz.isArray();

		final Type componentType = ReflectionUilities.getComponentType(type);
		final Class<?> componentClazz = clazz.getComponentType();
		final List<Object> elements = new ArrayList<>();

		while (parser.next() != Event.END_ARRAY) {
			final Object elem = context.deserialize(componentType, parser);
			elements.add(elem);
		}

		final Object array = Array.newInstance(componentClazz, elements.size());

		int index = 0;
		for (final Object element : elements) {
			Array.set(array, index++, element);
		}

		return array;
	}
}
