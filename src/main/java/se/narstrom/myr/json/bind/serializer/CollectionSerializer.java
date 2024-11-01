package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.ReflectionUilities;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#collections
public final class CollectionSerializer implements JsonbSerializer<Collection<?>>, JsonbDeserializer<Collection<?>> {

	@Override
	public Collection<?> deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		final Class<?> clazz = ReflectionUilities.getClass(type);
		final Type elementType = findElementType(type);
		if (elementType == null)
			throw new ClassCastException("Not a collection?");

		if (parser.currentEvent() != Event.START_ARRAY)
			throw new JsonbException("Not an array");

		final Collection<?> instance;
		try {
			instance = (Collection<?>) clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException | ClassCastException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}

		while (parser.next() != Event.END_ARRAY) {
			instance.add(ctx.deserialize(elementType, parser));
		}

		return instance;
	}

	@Override
	public void serialize(final Collection<?> obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.writeStartArray();
		for (final Object element : obj) {
			ctx.serialize(element, generator);
		}
		generator.writeEnd();
	}

	private Type findElementType(final Type type) {
		final Class<?> clazz;
		if (type instanceof ParameterizedType parameterized) {
			clazz = (Class<?>) parameterized.getRawType();
			if (clazz == Collection.class)
				return parameterized.getActualTypeArguments()[0];
		} else if (type instanceof Class<?>) {
			clazz = (Class<?>) type;
			if (clazz == Collection.class)
				return Object.class;
		} else {
			throw new JsonbException("Unsupported type implementation: " + type.getClass().getName());
		}

		for (final Type interfaceType : clazz.getGenericInterfaces()) {
			final Type found = findElementType(interfaceType);
			if (found != null)
				return found;
		}

		final Type superType = clazz.getGenericSuperclass();
		if (superType != null) {
			final Type found = findElementType(superType);
			if (found != null)
				return found;
		}

		return null;
	}
}
