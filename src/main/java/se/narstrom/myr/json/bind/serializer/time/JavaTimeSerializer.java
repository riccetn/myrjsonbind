package se.narstrom.myr.json.bind.serializer.time;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.Optional;

import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.MyrJsonbContext;

public final class JavaTimeSerializer<T extends TemporalAccessor> implements JsonbSerializer<T>, JsonbDeserializer<T> {
	private final DateTimeFormatter defaultFormatter;

	private final TemporalQuery<T> temporalQuery;

	public JavaTimeSerializer(final DateTimeFormatter formatter, final TemporalQuery<T> temporalQuery) {
		this.defaultFormatter = formatter;
		this.temporalQuery = temporalQuery;
	}

	@Override
	public T deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		final DateTimeFormatter formatter = findFormatter((MyrJsonbContext) context);

		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		try {
			return formatter.parse(parser.getString(), temporalQuery);
		} catch (final DateTimeParseException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final T obj, final JsonGenerator generator, final SerializationContext context) {
		final DateTimeFormatter formatter = findFormatter((MyrJsonbContext) context);

		try {
			generator.write(formatter.format(obj));
		} catch (final DateTimeException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	private DateTimeFormatter findFormatter(final MyrJsonbContext context) {
		final Optional<String> format = context.getConfig().getProperty(JsonbConfig.DATE_FORMAT).map(obj -> (String) obj);
		final Locale locale = findLocale(context);

		final DateTimeFormatter formatter;
		if (format.isPresent())
			formatter = DateTimeFormatter.ofPattern(format.get(), locale);
		else
			formatter = defaultFormatter.withLocale(locale);
		return formatter;
	}

	private Locale findLocale(final MyrJsonbContext context) {
		return context.getConfig().getProperty(JsonbConfig.LOCALE).map(obj -> (Locale) obj).orElseGet(Locale::getDefault);
	}
}
