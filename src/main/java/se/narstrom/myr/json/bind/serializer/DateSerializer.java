package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

// https://jakarta.ee/specifications/jsonb/3.0/jakarta-jsonb-spec-3.0#java-util-date-calendar-gregoriancalendar
public final class DateSerializer implements JsonbSerializer<Date>, JsonbDeserializer<Date> {

	@Override
	public Date deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		try {
			return Date.from(DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC).parse(parser.getString(), Instant::from));
		} catch (final DateTimeParseException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final Date obj, final JsonGenerator generator, final SerializationContext ctx) {
		try {
			generator.write(DateTimeFormatter.ISO_DATE_TIME.format(obj.toInstant().atZone(ZoneOffset.UTC)));
		} catch (final DateTimeException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

}
