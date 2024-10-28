package se.narstrom.myr.json.bind;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.serializer.BooleanSerializer;
import se.narstrom.myr.json.bind.serializer.ByteSerializer;
import se.narstrom.myr.json.bind.serializer.CharacterSerializer;
import se.narstrom.myr.json.bind.serializer.DoubleSerializer;
import se.narstrom.myr.json.bind.serializer.EnumSerializer;
import se.narstrom.myr.json.bind.serializer.FloatSerializer;
import se.narstrom.myr.json.bind.serializer.IntegerSerializer;
import se.narstrom.myr.json.bind.serializer.LongSerializer;
import se.narstrom.myr.json.bind.serializer.NumberSerializer;
import se.narstrom.myr.json.bind.serializer.ShortSerializer;
import se.narstrom.myr.json.bind.serializer.StringSerializer;

public final class MyrJsonb implements Jsonb, SerializationContext, DeserializationContext {
	private final JsonbConfig config;
	private final JsonProvider jsonp;

	private Map<Class<?>, JsonbSerializer<?>> serializers = Map.ofEntries(
	// @formatter:off
		Map.entry(Object.class, new DefaultSerializer()),
		Map.entry(Enum.class, new EnumSerializer()),
		Map.entry(Boolean.class, new BooleanSerializer()),
		Map.entry(Boolean.TYPE, new BooleanSerializer()),
		Map.entry(Byte.class, new ByteSerializer()),
		Map.entry(Byte.TYPE, new ByteSerializer()),
		Map.entry(Character.class, new CharacterSerializer()),
		Map.entry(Character.TYPE, new CharacterSerializer()),
		Map.entry(Double.class, new DoubleSerializer()),
		Map.entry(Double.TYPE, new DoubleSerializer()),
		Map.entry(Float.class, new FloatSerializer()),
		Map.entry(Float.TYPE, new FloatSerializer()),
		Map.entry(Integer.class, new IntegerSerializer()),
		Map.entry(Integer.TYPE, new IntegerSerializer()),
		Map.entry(Long.class, new LongSerializer()),
		Map.entry(Long.TYPE, new LongSerializer()),
		Map.entry(Number.class, new NumberSerializer()),
		Map.entry(Short.class, new ShortSerializer()),
		Map.entry(Short.TYPE, new ShortSerializer()),
		Map.entry(String.class, new StringSerializer())
	// @formatter:on
	);

	private Map<Class<?>, JsonbDeserializer<?>> deserializers = Map.ofEntries(
	// @formatter:off
		Map.entry(Object.class, new DefaultDeserializer()),
		Map.entry(Enum.class, new EnumSerializer()),
		Map.entry(Boolean.class, new BooleanSerializer()),
		Map.entry(Boolean.TYPE, new BooleanSerializer()),
		Map.entry(Byte.class, new ByteSerializer()),
		Map.entry(Byte.TYPE, new ByteSerializer()),
		Map.entry(Character.class, new CharacterSerializer()),
		Map.entry(Character.TYPE, new CharacterSerializer()),
		Map.entry(Double.class, new DoubleSerializer()),
		Map.entry(Double.TYPE, new DoubleSerializer()),
		Map.entry(Float.class, new FloatSerializer()),
		Map.entry(Float.TYPE, new FloatSerializer()),
		Map.entry(Integer.class, new IntegerSerializer()),
		Map.entry(Integer.TYPE, new IntegerSerializer()),
		Map.entry(Long.class, new LongSerializer()),
		Map.entry(Long.TYPE, new LongSerializer()),
		Map.entry(Number.class, new NumberSerializer()),
		Map.entry(Short.class, new ShortSerializer()),
		Map.entry(Short.TYPE, new ShortSerializer()),
		Map.entry(String.class, new StringSerializer())
	// @formatter:on
	);

	public MyrJsonb(final JsonbConfig config, final JsonProvider jsonp) {
		this.config = config;
		this.jsonp = Objects.requireNonNull(jsonp);
	}

	@Override
	public void close() throws Exception {
		/* Nothing to do */
	}

	@Override
	public <T> T fromJson(final InputStream stream, final Class<T> type) throws JsonbException {
		try (final JsonParser parser = jsonp.createParser(stream)) {
			return deserialize((Type) type, parser);
		}
	}

	@Override
	public <T> T fromJson(final InputStream stream, final Type type) throws JsonbException {
		try (final JsonParser parser = jsonp.createParser(stream)) {
			return deserialize(type, parser);
		}
	}

	@Override
	public <T> T fromJson(final Reader reader, final Class<T> type) throws JsonbException {
		try (final JsonParser parser = jsonp.createParser(reader)) {
			return deserialize((Type) type, parser);
		}
	}

	@Override
	public <T> T fromJson(final Reader reader, final Type type) throws JsonbException {
		try (final JsonParser parser = jsonp.createParser(reader)) {
			return deserialize(type, parser);
		}
	}

	@Override
	public <T> T fromJson(final String str, final Class<T> type) throws JsonbException {
		try (final JsonParser parser = jsonp.createParser(new StringReader(str))) {
			return deserialize((Type) type, parser);
		}
	}

	@Override
	public <T> T fromJson(final String str, final Type type) throws JsonbException {
		try (final JsonParser parser = jsonp.createParser(new StringReader(str))) {
			return deserialize(type, parser);
		}
	}

	@Override
	public String toJson(final Object object) throws JsonbException {
		final StringWriter writer = new StringWriter();
		try (final JsonGenerator generator = jsonp.createGenerator(writer)) {
			serialize(object, object.getClass(), generator);
		}
		return writer.toString();
	}

	@Override
	public void toJson(final Object object, final OutputStream stream) throws JsonbException {
		try (final JsonGenerator generator = jsonp.createGenerator(stream)) {
			serialize(object, object.getClass(), generator);
		}
	}

	@Override
	public String toJson(final Object object, final Type type) throws JsonbException {
		final StringWriter writer = new StringWriter();
		try (final JsonGenerator generator = jsonp.createGenerator(writer)) {
			serialize(object, type, generator);
		}
		return writer.toString();
	}

	@Override
	public void toJson(final Object object, final Type type, final OutputStream stream) throws JsonbException {
		try (final JsonGenerator generator = jsonp.createGenerator(stream)) {
			serialize(object, type, generator);
		}
	}

	@Override
	public void toJson(final Object object, final Type type, final Writer writer) throws JsonbException {
		try (final JsonGenerator generator = jsonp.createGenerator(writer)) {
			serialize(object, type, generator);
		}
	}

	@Override
	public void toJson(final Object object, final Writer writer) throws JsonbException {
		try (final JsonGenerator generator = jsonp.createGenerator(writer)) {
			serialize(object, object.getClass(), generator);
		}

	}

	@Override
	public <T> T deserialize(final Class<T> clazz, final JsonParser parser) {
		return deserialize((Type) clazz, parser);
	}

	@Override
	public <T> T deserialize(final Type type, final JsonParser parser) throws JsonbException {
		if (parser.currentEvent() == null || parser.currentEvent() == Event.KEY_NAME)
			parser.next();
		if (parser.currentEvent() == Event.VALUE_NULL)
			return null;
		Class<?> clazz = (Class<?>) type;
		JsonbDeserializer<?> deserializer;
		while ((deserializer = deserializers.get(clazz)) == null) {
			clazz = clazz.getSuperclass();
			if (clazz == null)
				clazz = Object.class;
		}
		return (T) deserializer.deserialize(parser, this, type);
	}

	@Override
	public <T> void serialize(final String key, final T object, final JsonGenerator generator) {
		generator.writeKey(key);
		if (object == null) {
			generator.writeNull();
		} else {
			serialize(object, object.getClass(), generator);
		}
	}

	@Override
	public <T> void serialize(final T object, final JsonGenerator generator) {
		if (object == null) {
			generator.writeNull();
		} else {
			serialize(object, object.getClass(), generator);
		}
	}

	private <T> void serialize(final T object, final Type type, final JsonGenerator generator) throws JsonbException {
		Class<?> clazz = (Class<?>) type;
		JsonbSerializer<?> serializer;
		while ((serializer = serializers.get(clazz)) == null) {
			clazz = clazz.getSuperclass();
			if (clazz == null)
				clazz = Object.class;
		}
		((JsonbSerializer<T>) serializer).serialize(object, generator, this);
	}
}
