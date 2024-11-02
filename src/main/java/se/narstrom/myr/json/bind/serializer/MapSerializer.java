package se.narstrom.myr.json.bind.serializer;

import java.awt.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
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

public final class MapSerializer implements JsonbSerializer<Map<String, ?>>, JsonbDeserializer<Map<String, ?>> {

	@Override
	public Map<String, ?> deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		final Class<?> clazz = ReflectionUilities.getClass(type);

		final Type[] keyAndValueTypes = getKeyAndValueTypes(type);
		if (keyAndValueTypes == null)
			throw new JsonbException("Not a map?");

		assert keyAndValueTypes.length == 2;
		final Type keyType = keyAndValueTypes[0];
		final Type valueType = keyAndValueTypes[1];

		if (keyType != String.class)
			throw new JsonbException("Map keys most be String, is " + keyType);

		if (parser.currentEvent() != Event.START_OBJECT)
			throw new JsonbException("Not an object");

		final Map<String, ?> map;
		try {
			map = (Map<String, ?>) getConstructor(clazz).newInstance();
		} catch (ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}

		while (parser.next() != Event.END_OBJECT) {
			assert parser.currentEvent() == Event.KEY_NAME;
			final String name = parser.getString();

			map.put(name, context.deserialize(valueType, parser));
		}

		return map;
	}

	@Override
	public void serialize(final Map<String, ?> map, final JsonGenerator generator, final SerializationContext context) {
		generator.writeStartObject();
		for (final Map.Entry<String, ?> entry : map.entrySet()) {
			context.serialize(entry.getKey(), entry.getValue(), generator);
		}
		generator.writeEnd();
	}

	private Constructor<?> getConstructor(final Class<?> type) throws ReflectiveOperationException {
		if (!type.isInterface())
			return type.getConstructor();

		else if (type == Map.class)
			return HashMap.class.getConstructor();
		else if (type == NavigableMap.class || type == SortedMap.class)
			return TreeMap.class.getConstructor();
		else
			throw new JsonbException("Unsupported interface type " + type);
	}

	private Type[] getKeyAndValueTypes(final Type type) {
		final Type genericMap;
		try {
			genericMap = ReflectionUilities.getGenericInterfaceType(type, Map.class);
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}

		if (genericMap instanceof ParameterizedType parameterized) {
			return parameterized.getActualTypeArguments();
		} else {
			return new Type[] { Object.class, Object.class };
		}
	}
}
