package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class JavaTimeSerializer<T extends TemporalAccessor> implements JsonbSerializer<T>, JsonbDeserializer<T> {
	private final DateTimeFormatter formatter;

	private final TemporalQuery<T> temporalQuery;

	public JavaTimeSerializer(final DateTimeFormatter formatter, final TemporalQuery<T> temporalQuery) {
		this.formatter = formatter;
		this.temporalQuery = temporalQuery;
	}

	@Override
	public T deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Not a string");
		try {
			return formatter.parse(parser.getString(), temporalQuery);
		} catch (final DateTimeParseException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final T obj, final JsonGenerator generator, final SerializationContext ctx) {
		try {
			generator.write(formatter.format(obj));
		} catch (final DateTimeException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

}
