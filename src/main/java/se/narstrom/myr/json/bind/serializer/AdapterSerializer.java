package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public final class AdapterSerializer<O, A> implements JsonbSerializer<O>, JsonbDeserializer<O> {
	final JsonbAdapter<O, A> adapter;

	public AdapterSerializer(final JsonbAdapter<O, A> adapter) {
		this.adapter = adapter;
	}

	@Override
	public O deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		final Type adapterType = ReflectionUtilities.getAncestorType(adapter.getClass(), JsonbAdapter.class);
		final Type adaptedType = ReflectionUtilities.getTypeArguments(adapterType)[1];

		final A adapted = context.deserialize(adaptedType, parser);
		try {
			return adapter.adaptFromJson(adapted);
		} catch (final Exception ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final O object, final JsonGenerator generator, final SerializationContext context) {
		final A adapted;
		try {
			adapted = adapter.adaptToJson(object);
		} catch (final Exception ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
		context.serialize(adapted, generator);
	}

}
