package se.narstrom.myr.json.bind.serializer.time;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
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
			if (str.indexOf('T') == -1) {
				return GregorianCalendar.from(LocalDate.parse(str, DateTimeFormatter.ISO_DATE).atTime(0, 0).atZone(ZoneOffset.UTC));
			} else {
				return GregorianCalendar.from(ZonedDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)));
			}
		} catch (final DateTimeException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final Calendar obj, final JsonGenerator generator, final SerializationContext ctx) {
		boolean hasDate = obj.isSet(Calendar.YEAR) && obj.isSet(Calendar.MONTH) && obj.isSet(Calendar.DAY_OF_MONTH);
		boolean hasTime = ((obj.isSet(Calendar.HOUR) && obj.isSet(Calendar.AM_PM)) || obj.isSet(Calendar.HOUR_OF_DAY)) && obj.isSet(Calendar.MINUTE);
		boolean hasSecond = hasTime && obj.isSet(Calendar.SECOND);
		boolean hasMilli = hasSecond && obj.isSet(Calendar.MILLISECOND);
		boolean hasZoneOffset = obj.isSet(Calendar.ZONE_OFFSET);

		final DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
		if (hasDate || !hasTime)
			builder.append(DateTimeFormatter.ISO_LOCAL_DATE);
		if (hasDate && hasTime)
			builder.appendLiteral('T');
		if (hasTime) {
			builder.appendValue(ChronoField.HOUR_OF_DAY, 2);
			builder.appendLiteral(':');
			builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
		}
		if (hasSecond) {
			builder.appendLiteral(':');
			builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
		}
		if (hasMilli) {
			builder.appendFraction(ChronoField.NANO_OF_SECOND, 0, 3, true);
		}
		builder.appendOffsetId();
		if (hasZoneOffset) {
			builder.appendLiteral('[');
			builder.appendZoneRegionId();
			builder.appendLiteral(']');
		}

		final DateTimeFormatter formatter = builder.toFormatter();

		if (obj instanceof GregorianCalendar gregorian) {
			generator.write(gregorian.toZonedDateTime().format(formatter));
		} else {
			generator.write(obj.toInstant().atZone(obj.getTimeZone().toZoneId()).format(formatter));
		}

	}

}
