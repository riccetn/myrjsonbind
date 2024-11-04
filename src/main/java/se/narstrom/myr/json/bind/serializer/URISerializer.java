package se.narstrom.myr.json.bind.serializer;

import java.lang.reflect.Type;
import java.net.URI;

import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;

public final class URISerializer implements JsonbSerializer<URI>, JsonbDeserializer<URI> {

	@Override
	public URI deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		return URI.create(parser.getString());
	}

	@Override
	public void serialize(final URI uri, final JsonGenerator generator, final SerializationContext context) {
		generator.write(uri.toString());
	}

}
