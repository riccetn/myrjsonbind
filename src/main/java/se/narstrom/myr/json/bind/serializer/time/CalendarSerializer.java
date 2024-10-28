package se.narstrom.myr.json.bind.serializer.time;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class CalendarSerializer implements JsonbSerializer<Calendar>, JsonbDeserializer<Calendar> {

	@Override
	public Calendar deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");

		final String str = parser.getString();

		try {
			return GregorianCalendar.from(ZonedDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)));
		} catch (final DateTimeException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final Calendar obj, final JsonGenerator generator, final SerializationContext ctx) {
		if (obj instanceof GregorianCalendar gregorian) {
			generator.write(gregorian.toZonedDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
		} else {
			generator.write(obj.toInstant().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME));
		}

	}

}
