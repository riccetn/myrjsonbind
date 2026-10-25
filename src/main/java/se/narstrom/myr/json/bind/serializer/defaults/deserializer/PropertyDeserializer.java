package se.narstrom.myr.json.bind.serializer.defaults.deserializer;

import jakarta.json.stream.JsonParser;
import se.narstrom.myr.json.bind.MyrJsonbContext;
import se.narstrom.myr.json.bind.serializer.defaults.prioperty.Property;

public interface PropertyDeserializer<T> {
	T deserializeProperty(final JsonParser parser, final MyrJsonbContext context, final Property property);
}
