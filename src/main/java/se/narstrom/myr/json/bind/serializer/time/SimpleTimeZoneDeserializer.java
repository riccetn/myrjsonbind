package se.narstrom.myr.json.bind.serializer.time;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.SimpleTimeZone;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class SimpleTimeZoneDeserializer implements JsonbDeserializer<SimpleTimeZone> {

	@Override
	public SimpleTimeZone deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");

		final String value = parser.getString();

		try {
			final ZoneId zoneId = ZoneId.of(value);
			return new SimpleTimeZone(zoneId.getRules().getStandardOffset(Instant.now()).getTotalSeconds() * 1000, zoneId.getId());
		} catch (final DateTimeException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}
}
