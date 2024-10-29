package se.narstrom.myr.json.bind.serializer.time;

import java.lang.reflect.Type;
import java.util.TimeZone;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class TimeZoneSerializer implements JsonbSerializer<TimeZone>, JsonbDeserializer<TimeZone> {

	@Override
	public TimeZone deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		final String zoneId = parser.getString();
		if (zoneId.length() == 3)
			throw new JsonbException("Deprecetad 3-latter zone id");
		return TimeZone.getTimeZone(parser.getString());
	}

	@Override
	public void serialize(final TimeZone obj, final JsonGenerator generator, final SerializationContext ctx) {
		final String zoneId = obj.getID();
		if (zoneId.length() == 3)
			throw new JsonbException("Deprecated 3-letter zone id");
		generator.write(obj.getID());
	}

}
