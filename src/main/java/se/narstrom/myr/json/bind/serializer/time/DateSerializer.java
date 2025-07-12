package se.narstrom.myr.json.bind.serializer.time;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
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

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-date-calendar-gregoriancalendar
public final class DateSerializer implements JsonbSerializer<Date>, JsonbDeserializer<Date> {

	@Override
	public Date deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		final DateTimeFormatter formatter = findFormatter((MyrJsonbContext) context);

		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		try {
			return Date.from(formatter.withZone(ZoneId.of("UTC")).parse(parser.getString(), Instant::from));
		} catch (final DateTimeParseException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final Date obj, final JsonGenerator generator, final SerializationContext context) {
		final DateTimeFormatter formatter = findFormatter((MyrJsonbContext) context);

		try {
			generator.write(formatter.format(obj.toInstant().atZone(ZoneId.of("UTC"))));
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
			formatter = DateTimeFormatter.ISO_DATE_TIME.withLocale(locale);
		return formatter;
	}

	private Locale findLocale(final MyrJsonbContext context) {
		return context.getConfig().getProperty(JsonbConfig.LOCALE).map(obj -> (Locale) obj).orElseGet(Locale::getDefault);
	}
}
