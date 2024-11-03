package se.narstrom.myr.json.bind;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
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
import se.narstrom.myr.json.bind.reflect.ReflectionUilities;
import se.narstrom.myr.json.bind.serializer.ArraySerializer;
import se.narstrom.myr.json.bind.serializer.CollectionSerializer;
import se.narstrom.myr.json.bind.serializer.DefaultDeserializer;
import se.narstrom.myr.json.bind.serializer.DefaultSerializer;
import se.narstrom.myr.json.bind.serializer.EnumSerializer;
import se.narstrom.myr.json.bind.serializer.JsonpSerializer;
import se.narstrom.myr.json.bind.serializer.MapSerializer;
import se.narstrom.myr.json.bind.serializer.basic.BooleanSerializer;
import se.narstrom.myr.json.bind.serializer.basic.ByteSerializer;
import se.narstrom.myr.json.bind.serializer.basic.CharacterSerializer;
import se.narstrom.myr.json.bind.serializer.basic.DoubleSerializer;
import se.narstrom.myr.json.bind.serializer.basic.FloatSerializer;
import se.narstrom.myr.json.bind.serializer.basic.IntegerSerializer;
import se.narstrom.myr.json.bind.serializer.basic.LongSerializer;
import se.narstrom.myr.json.bind.serializer.basic.NumberSerializer;
import se.narstrom.myr.json.bind.serializer.basic.ShortSerializer;
import se.narstrom.myr.json.bind.serializer.basic.StringSerializer;
import se.narstrom.myr.json.bind.serializer.time.CalendarSerializer;
import se.narstrom.myr.json.bind.serializer.time.DateSerializer;
import se.narstrom.myr.json.bind.serializer.time.DurationSerializer;
import se.narstrom.myr.json.bind.serializer.time.JavaTimeSerializer;
import se.narstrom.myr.json.bind.serializer.time.PeriodSerializer;
import se.narstrom.myr.json.bind.serializer.time.SimpleTimeZoneDeserializer;
import se.narstrom.myr.json.bind.serializer.time.TimeZoneSerializer;
import se.narstrom.myr.json.bind.serializer.time.ZoneIdSerializer;

public final class MyrJsonbContext implements Jsonb, SerializationContext, DeserializationContext {
	private static final Logger LOG = Logger.getLogger(MyrJsonbContext.class.getName());
	private final JsonbConfig config;
	private final JsonProvider jsonp;

	private Map<Class<?>, JsonbSerializer<?>> serializers = Map.ofEntries(
	// @formatter:off
		// 3.3 Basic Java Types
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#basic-java-types
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
		Map.entry(String.class, new StringSerializer()),

		// 3.5 Dates
		// 3.5.1 java.uril.Data, Calendar, GregorianCalendar
		Map.entry(Date.class, new DateSerializer()),
		Map.entry(Calendar.class, new CalendarSerializer()),

		// 3.5.2. java.util.TimeZone, SimpleTimeZone
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-timezone-simpletimezone
		Map.entry(TimeZone.class, new TimeZoneSerializer()),

		// 3.5.3 java.time.*
		Map.entry(Instant.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_INSTANT, Instant::from)),
		Map.entry(LocalDate.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE, LocalDate::from)),
		Map.entry(LocalTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_TIME, LocalTime::from)),
		Map.entry(LocalDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from)),
		Map.entry(ZonedDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_ZONED_DATE_TIME, ZonedDateTime::from)),
		Map.entry(OffsetDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_DATE_TIME, OffsetDateTime::from)),
		Map.entry(OffsetTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_TIME, OffsetTime::from)),
		Map.entry(Duration.class, new DurationSerializer()),
		Map.entry(Period.class, new PeriodSerializer()),
		Map.entry(ZoneId.class, new ZoneIdSerializer()),

		// 3.9 Enum
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#enum
		Map.entry(Enum.class, new EnumSerializer()),

		// 3.11 Collections
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#collections
		Map.entry(Collection.class, new CollectionSerializer()),
		Map.entry(Map.class, new MapSerializer()),

		// 3.20 JSON Processing integration
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#json-processing-integration
		Map.entry(JsonArray.class, new JsonpSerializer()),
		Map.entry(JsonObject.class, new JsonpSerializer()),
		Map.entry(JsonValue.class, new JsonpSerializer())
	// @formatter:on
	);

	private Map<Class<?>, JsonbDeserializer<?>> deserializers = Map.ofEntries(
	// @formatter:off
		// 3.3 Basic Java Types
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#basic-java-types
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
		Map.entry(String.class, new StringSerializer()),

		// 3.5 Dates
		// 3.5.1 java.uril.Data, Calendar, GregorianCalendar
		Map.entry(Date.class, new DateSerializer()),
		Map.entry(Calendar.class, new CalendarSerializer()),

		// 3.5.2. java.util.TimeZone, SimpleTimeZone
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-timezone-simpletimezone
		Map.entry(TimeZone.class, new TimeZoneSerializer()),
		Map.entry(SimpleTimeZone.class, new SimpleTimeZoneDeserializer()),

		// 3.5.3 java.time.*
		Map.entry(Instant.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_INSTANT, Instant::from)),
		Map.entry(LocalDate.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE, LocalDate::from)),
		Map.entry(LocalTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_TIME, LocalTime::from)),
		Map.entry(LocalDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from)),
		Map.entry(ZonedDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_ZONED_DATE_TIME, ZonedDateTime::from)),
		Map.entry(OffsetDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_DATE_TIME, OffsetDateTime::from)),
		Map.entry(OffsetTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_TIME, OffsetTime::from)),
		Map.entry(Duration.class, new DurationSerializer()),
		Map.entry(Period.class, new PeriodSerializer()),
		Map.entry(ZoneId.class, new ZoneIdSerializer()),

		// 3.9 Enum
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#enum
		Map.entry(Enum.class, new EnumSerializer()),

		// 3.11 Collections
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#collections
		Map.entry(Collection.class, new CollectionSerializer()),
		Map.entry(Map.class, new MapSerializer()),

		// 3.20 JSON Processing integration
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#json-processing-integration
		Map.entry(JsonArray.class, new JsonpSerializer()),
		Map.entry(JsonObject.class, new JsonpSerializer()),
		Map.entry(JsonValue.class, new JsonpSerializer())
	// @formatter:on
	);

	// 3.7 Java Class
	// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-class
	private JsonbSerializer<?> defaultSerialzer = new DefaultSerializer();
	private JsonbDeserializer<?> defaultDeserialzer = new DefaultDeserializer();

	// 3.12 Array
	// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#arrays
	private JsonbSerializer<?> arraySerializer = new ArraySerializer();
	private JsonbDeserializer<?> arrayDeserializer = new ArraySerializer();

	public MyrJsonbContext(final JsonbConfig config, final JsonProvider jsonp) {
		this.config = config;
		this.jsonp = Objects.requireNonNull(jsonp);
	}

	@Override
	public void close() throws Exception {
		/* Nothing to do */
	}

	public JsonbConfig getConfig() {
		return config;
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

//		if (parser.currentEvent() == Event.VALUE_NULL)
//			return null;

		final JsonbDeserializer<?> deserializer = findDeserializer(type);

		LOG.fine(() -> String.format("Deserializing %s with %s", type, deserializer.getClass().getName()));

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
		final JsonbSerializer<?> serializer = findSerializer(type);

		LOG.fine(() -> String.format("Serializing %s with %s", type, serializer.getClass().getName()));

		((JsonbSerializer<T>) serializer).serialize(object, generator, this);
	}

	private JsonbDeserializer<?> findDeserializer(final Type type) {
		final Class<?> clazz = ReflectionUilities.getClass(type);

		{
			final JsonbDeserializer<?> candidate = deserializers.get(clazz);
			if (candidate != null)
				return candidate;
		}

		for (Class<?> superClazz = clazz.getSuperclass(); superClazz != null && superClazz != Object.class; superClazz = superClazz.getSuperclass()) {
			final JsonbDeserializer<?> candidate = deserializers.get(superClazz);
			if (candidate != null)
				return candidate;
		}

		for (Class<?> superClazz = clazz; superClazz != null && superClazz != Object.class; superClazz = superClazz.getSuperclass()) {
			final Deque<Class<?>> q = new ArrayDeque<>();
			Collections.addAll(q, superClazz.getInterfaces());

			while (!q.isEmpty()) {
				final Class<?> interfaceClazz = q.poll();

				final JsonbDeserializer<?> candidate = deserializers.get(interfaceClazz);
				if (candidate != null)
					return candidate;

				Collections.addAll(q, interfaceClazz.getInterfaces());
			}
		}

		if (clazz.isArray())
			return arrayDeserializer;

		return defaultDeserialzer;
	}

	private JsonbSerializer<?> findSerializer(final Type type) {
		final Class<?> clazz = ReflectionUilities.getClass(type);

		{
			final JsonbSerializer<?> candidate = serializers.get(clazz);
			if (candidate != null)
				return candidate;
		}

		for (Class<?> superClazz = clazz.getSuperclass(); superClazz != null && superClazz != Object.class; superClazz = superClazz.getSuperclass()) {
			final JsonbSerializer<?> candidate = serializers.get(superClazz);
			if (candidate != null)
				return candidate;
		}

		for (Class<?> superClazz = clazz; superClazz != null && superClazz != Object.class; superClazz = superClazz.getSuperclass()) {
			final Deque<Class<?>> q = new ArrayDeque<>();
			Collections.addAll(q, superClazz.getInterfaces());

			while (!q.isEmpty()) {
				final Class<?> interfaceClazz = q.poll();

				final JsonbSerializer<?> candidate = serializers.get(interfaceClazz);
				if (candidate != null)
					return candidate;

				Collections.addAll(q, interfaceClazz.getInterfaces());
			}
		}

		if (clazz.isArray())
			return arraySerializer;

		return defaultSerialzer;
	}
}
