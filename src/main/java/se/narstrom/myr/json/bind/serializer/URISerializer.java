package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.net.URI;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class URISerializer implements JsonbSerializer<URI>, JsonbDeserializer<URI> {

	@Override
	public URI deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Expected a string found " + parser.currentEvent());
		return URI.create(parser.getString());
	}

	@Override
	public void serialize(final URI uri, final JsonGenerator generator, final SerializationContext context) {
		generator.write(uri.toString());
	}

}
