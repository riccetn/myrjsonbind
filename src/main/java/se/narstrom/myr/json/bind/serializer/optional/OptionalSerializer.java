package se.narstrom.myr.json.bind.serializer.optional;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public final class OptionalSerializer implements JsonbSerializer<Optional<?>>, JsonbDeserializer<Optional<?>> {

	@Override
	public Optional<?> deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (parser.currentEvent() == Event.VALUE_NULL)
			return Optional.empty();

		final Type optionalType = ReflectionUtilities.getAncestorType(type, Optional.class);

		if (optionalType instanceof ParameterizedType parameterized) {
			final Type innerType = parameterized.getActualTypeArguments()[0];
			return Optional.of(context.deserialize(innerType, parser));
		} else {
			return Optional.of(context.deserialize(Object.class, parser));
		}
	}

	@Override
	public void serialize(final Optional<?> optional, final JsonGenerator generator, final SerializationContext context) {
		if (optional.isEmpty())
			generator.writeNull();
		else
			context.serialize(optional.get(), generator);
	}

}
