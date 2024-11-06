package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class URLSerializer implements JsonbSerializer<URL>, JsonbDeserializer<URL> {

	@Override
	public URL deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		if (parser.currentEvent() != Event.VALUE_STRING)
			throw new JsonbException("Expected string found: " + parser.currentEvent());
		try {
			return URI.create(parser.getString()).toURL();
		} catch (final MalformedURLException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	@Override
	public void serialize(final URL url, final JsonGenerator generator, final SerializationContext context) {
		try {
			generator.write(url.toURI().toString());
		} catch (final URISyntaxException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

}
