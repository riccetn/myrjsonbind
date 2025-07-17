package se.narstrom.myr.json.bind.serializer.defaults;

import jakarta.json.stream.JsonParser;
import se.narstrom.myr.json.bind.MyrJsonbContext;

public interface PropertyDeserializer<T> {
	T deserializeProperty(final JsonParser parser, final MyrJsonbContext context, final Property property);
}
