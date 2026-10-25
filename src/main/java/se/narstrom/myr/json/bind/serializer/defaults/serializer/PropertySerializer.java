package se.narstrom.myr.json.bind.serializer.defaults.serializer;

import jakarta.json.stream.JsonGenerator;
import se.narstrom.myr.json.bind.MyrJsonbContext;
import se.narstrom.myr.json.bind.serializer.defaults.prioperty.ReadableProperty;

public interface PropertySerializer<T> {
	void serializeProperty(final T object, final JsonGenerator generator, final MyrJsonbContext context, final ReadableProperty property);
}
