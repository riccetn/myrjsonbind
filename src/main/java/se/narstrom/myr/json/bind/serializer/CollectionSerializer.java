package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
			instance = (Collection<?>) getConstructor(clazz).newInstance();
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

	private Constructor<?> getConstructor(final Class<?> type) throws ReflectiveOperationException {
		if (!type.isInterface())
			return type.getConstructor();

		else if (type == Collection.class || type == List.class)
			return ArrayList.class.getConstructor();
		else if (type == Set.class)
			return HashSet.class.getConstructor();
		else if (type == NavigableSet.class || type == SortedSet.class)
			return TreeSet.class.getConstructor();
		else if (type == Deque.class)
			return ArrayDeque.class.getConstructor();
		else
			throw new JsonbException("Unsupported interface type " + type);
	}

	private Type findElementType(final Type type) {
		final Type genericCollection;
		try {
			genericCollection = ReflectionUilities.getGenericInterfaceType(type, Collection.class);
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}

		if (genericCollection instanceof ParameterizedType parameterized) {
			return parameterized.getActualTypeArguments()[0];
		} else {
			return Object.class;
		}
	}
}
