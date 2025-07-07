package se.narstrom.myr.json.bind;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.logging.Logger;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.reflect.ReflectionUilities;
import se.narstrom.myr.json.bind.serializer.AdapterSerializer;
import se.narstrom.myr.json.bind.serializer.ArraySerializer;
import se.narstrom.myr.json.bind.serializer.BigDecimalSerializer;
import se.narstrom.myr.json.bind.serializer.BigIntegerSerializer;
import se.narstrom.myr.json.bind.serializer.EnumSerializer;
import se.narstrom.myr.json.bind.serializer.JsonpSerializer;
import se.narstrom.myr.json.bind.serializer.URISerializer;
import se.narstrom.myr.json.bind.serializer.URLSerializer;
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
import se.narstrom.myr.json.bind.serializer.collections.CollectionSerializer;
import se.narstrom.myr.json.bind.serializer.collections.MapSerializer;
import se.narstrom.myr.json.bind.serializer.defaults.DefaultDeserializer;
import se.narstrom.myr.json.bind.serializer.defaults.DefaultSerializer;
import se.narstrom.myr.json.bind.serializer.optional.OptionalDoubleSerializer;
import se.narstrom.myr.json.bind.serializer.optional.OptionalIntSerializer;
import se.narstrom.myr.json.bind.serializer.optional.OptionalLongSerializer;
import se.narstrom.myr.json.bind.serializer.optional.OptionalSerializer;
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

	private static final Map<Event, Type> DEFAULT_TYPES = Map.ofEntries(
	// @formatter:off
		Map.entry(Event.START_OBJECT, LinkedHashMap.class),
		Map.entry(Event.START_ARRAY, ArrayList.class),
		Map.entry(Event.VALUE_STRING, String.class),
		Map.entry(Event.VALUE_NUMBER, BigDecimal.class),
		Map.entry(Event.VALUE_TRUE, Boolean.class),
		Map.entry(Event.VALUE_FALSE, Boolean.class),
		Map.entry(Event.VALUE_NULL, Object.class)
	// @formatter:on
	);

	private final JsonbConfig config;
	private final JsonProvider jsonp;

	private Map<Class<?>, JsonbSerializer<?>> serializers = new HashMap<>();
	private Map<Class<?>, JsonbDeserializer<?>> deserializers = new HashMap<>();

	{
		// 3.3 Basic Java Types
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#basic-java-types
		serializers.put(Boolean.class, new BooleanSerializer());
		serializers.put(Boolean.TYPE, new BooleanSerializer());
		serializers.put(Byte.class, new ByteSerializer());
		serializers.put(Byte.TYPE, new ByteSerializer());
		serializers.put(Character.class, new CharacterSerializer());
		serializers.put(Character.TYPE, new CharacterSerializer());
		serializers.put(Double.class, new DoubleSerializer());
		serializers.put(Double.TYPE, new DoubleSerializer());
		serializers.put(Float.class, new FloatSerializer());
		serializers.put(Float.TYPE, new FloatSerializer());
		serializers.put(Integer.class, new IntegerSerializer());
		serializers.put(Integer.TYPE, new IntegerSerializer());
		serializers.put(Long.class, new LongSerializer());
		serializers.put(Long.TYPE, new LongSerializer());
		serializers.put(Number.class, new NumberSerializer());
		serializers.put(Short.class, new ShortSerializer());
		serializers.put(Short.TYPE, new ShortSerializer());
		serializers.put(String.class, new StringSerializer());

		// 3.4.1 java.math.BigInteger, BigDecimal
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-math-biginteger-bigdecimal
		serializers.put(BigDecimal.class, new BigDecimalSerializer());
		serializers.put(BigInteger.class, new BigIntegerSerializer());

		// 3.4.2 java.net.URL, URI
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-net-url-uri
		serializers.put(URI.class, new URISerializer());
		serializers.put(URL.class, new URLSerializer());

		// 3.4.3 java.util.Optional, OptionalInt, OptionalLong, OptionalDouble
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-optional-optionalint-optionallong-optionaldouble
		serializers.put(Optional.class, new OptionalSerializer());
		serializers.put(OptionalDouble.class, new OptionalDoubleSerializer());
		serializers.put(OptionalInt.class, new OptionalIntSerializer());
		serializers.put(OptionalLong.class, new OptionalLongSerializer());

		// 3.5 Dates
		// 3.5.1 java.uril.Data, Calendar, GregorianCalendar
		serializers.put(Date.class, new DateSerializer());
		serializers.put(Calendar.class, new CalendarSerializer());

		// 3.5.2. java.util.TimeZone, SimpleTimeZone
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-timezone-simpletimezone
		serializers.put(TimeZone.class, new TimeZoneSerializer());

		// 3.5.3 java.time.*
		serializers.put(Instant.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_INSTANT, Instant::from));
		serializers.put(LocalDate.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE, LocalDate::from));
		serializers.put(LocalTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_TIME, LocalTime::from));
		serializers.put(LocalDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from));
		serializers.put(ZonedDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_ZONED_DATE_TIME, ZonedDateTime::from));
		serializers.put(OffsetDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_DATE_TIME, OffsetDateTime::from));
		serializers.put(OffsetTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_TIME, OffsetTime::from));
		serializers.put(Duration.class, new DurationSerializer());
		serializers.put(Period.class, new PeriodSerializer());
		serializers.put(ZoneId.class, new ZoneIdSerializer());

		// 3.9 Enum
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#enum
		serializers.put(Enum.class, new EnumSerializer());

		// 3.11 Collections
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#collections
		serializers.put(Collection.class, new CollectionSerializer());
		serializers.put(Map.class, new MapSerializer());

		// 3.20 JSON Processing integration
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#json-processing-integration
		serializers.put(JsonArray.class, new JsonpSerializer());
		serializers.put(JsonObject.class, new JsonpSerializer());
		serializers.put(JsonValue.class, new JsonpSerializer());

		// 3.3 Basic Java Types
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#basic-java-types
		deserializers.put(Boolean.class, new BooleanSerializer());
		deserializers.put(Boolean.TYPE, new BooleanSerializer());
		deserializers.put(Byte.class, new ByteSerializer());
		deserializers.put(Byte.TYPE, new ByteSerializer());
		deserializers.put(Character.class, new CharacterSerializer());
		deserializers.put(Character.TYPE, new CharacterSerializer());
		deserializers.put(Double.class, new DoubleSerializer());
		deserializers.put(Double.TYPE, new DoubleSerializer());
		deserializers.put(Float.class, new FloatSerializer());
		deserializers.put(Float.TYPE, new FloatSerializer());
		deserializers.put(Integer.class, new IntegerSerializer());
		deserializers.put(Integer.TYPE, new IntegerSerializer());
		deserializers.put(Long.class, new LongSerializer());
		deserializers.put(Long.TYPE, new LongSerializer());
		deserializers.put(Number.class, new NumberSerializer());
		deserializers.put(Short.class, new ShortSerializer());
		deserializers.put(Short.TYPE, new ShortSerializer());
		deserializers.put(String.class, new StringSerializer());

		// 3.4.1 java.math.BigInteger, BigDecimal
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-math-biginteger-bigdecimal
		deserializers.put(BigDecimal.class, new BigDecimalSerializer());
		deserializers.put(BigInteger.class, new BigIntegerSerializer());

		// 3.4.2 java.net.URL, URI
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-net-url-uri
		deserializers.put(URI.class, new URISerializer());
		deserializers.put(URL.class, new URLSerializer());

		// 3.4.3 java.util.Optional, OptionalInt, OptionalLong, OptionalDouble
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-optional-optionalint-optionallong-optionaldouble
		deserializers.put(Optional.class, new OptionalSerializer());
		deserializers.put(OptionalDouble.class, new OptionalDoubleSerializer());
		deserializers.put(OptionalInt.class, new OptionalIntSerializer());
		deserializers.put(OptionalLong.class, new OptionalLongSerializer());

		// 3.5 Dates
		// 3.5.1 java.uril.Data, Calendar, GregorianCalendar
		deserializers.put(Date.class, new DateSerializer());
		deserializers.put(Calendar.class, new CalendarSerializer());

		// 3.5.2. java.util.TimeZone, SimpleTimeZone
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-timezone-simpletimezone
		deserializers.put(TimeZone.class, new TimeZoneSerializer());
		deserializers.put(SimpleTimeZone.class, new SimpleTimeZoneDeserializer());

		// 3.5.3 java.time.*
		deserializers.put(Instant.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_INSTANT, Instant::from));
		deserializers.put(LocalDate.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE, LocalDate::from));
		deserializers.put(LocalTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_TIME, LocalTime::from));
		deserializers.put(LocalDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_LOCAL_DATE_TIME, LocalDateTime::from));
		deserializers.put(ZonedDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_ZONED_DATE_TIME, ZonedDateTime::from));
		deserializers.put(OffsetDateTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_DATE_TIME, OffsetDateTime::from));
		deserializers.put(OffsetTime.class, new JavaTimeSerializer<>(DateTimeFormatter.ISO_OFFSET_TIME, OffsetTime::from));
		deserializers.put(Duration.class, new DurationSerializer());
		deserializers.put(Period.class, new PeriodSerializer());
		deserializers.put(ZoneId.class, new ZoneIdSerializer());

		// 3.9 Enum
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#enum
		deserializers.put(Enum.class, new EnumSerializer());

		// 3.11 Collections
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#collections
		deserializers.put(Collection.class, new CollectionSerializer());
		deserializers.put(Map.class, new MapSerializer());

		// 3.20 JSON Processing integration
		// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#json-processing-integration
		deserializers.put(JsonArray.class, new JsonpSerializer());
		deserializers.put(JsonObject.class, new JsonpSerializer());
		deserializers.put(JsonValue.class, new JsonpSerializer());
	}

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

		installAdapters(config.getProperty(JsonbConfig.ADAPTERS).map(JsonbAdapter[].class::cast).orElseGet(() -> new JsonbAdapter[0]));
	}

	@Override
	public void close() throws Exception {
		/* Nothing to do */
	}

	public JsonbConfig getConfig() {
		return config;
	}

	public JsonProvider getJsonpProvider() {
		return jsonp;
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

		final Type actualType;
		if (type == Object.class) {
			actualType = DEFAULT_TYPES.get(parser.currentEvent());
			if (actualType == null)
				throw new JsonbException("Parser in wrong state: " + parser.currentEvent());
		} else {
			actualType = type;
		}

		final JsonbDeserializer<?> deserializer = findDeserializer(actualType, parser);

		LOG.fine(() -> String.format("Deserializing %s with %s", actualType, deserializer.getClass().getName()));

		return (T) deserializer.deserialize(parser, this, actualType);
	}

	@Override
	public <T> void serialize(final String key, final T object, final JsonGenerator generator) {
		if (object == null) {
			generator.writeNull(key);
		} else {
			generator.writeKey(key);
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

	private void installAdapters(final JsonbAdapter<?, ?>[] adapters) {
		for (final JsonbAdapter<?, ?> adapter : adapters) {
			final Type adapterType = ReflectionUilities.getAncestorType(adapter.getClass(), JsonbAdapter.class);
			final Type originalType = ReflectionUilities.getTypeArguments(adapterType)[0];
			final Class<?> originalRawType = ReflectionUilities.getRawType(originalType);

			final AdapterSerializer<?, ?> serializer = new AdapterSerializer<>(adapter);

			serializers.put(originalRawType, serializer);
			deserializers.put(originalRawType, serializer);
		}
	}

	private <T> void serialize(final T object, final Type type, final JsonGenerator generator) throws JsonbException {
		final JsonbSerializer<?> serializer = findSerializer(type);

		LOG.fine(() -> String.format("Serializing %s with %s", type, serializer.getClass().getName()));

		((JsonbSerializer<T>) serializer).serialize(object, generator, this);
	}

	private JsonbDeserializer<?> findDeserializer(final Type type, final JsonParser parser) {
		Class<?> clazz = ReflectionUilities.getRawType(type);

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
		final Class<?> clazz = ReflectionUilities.getRawType(type);

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
