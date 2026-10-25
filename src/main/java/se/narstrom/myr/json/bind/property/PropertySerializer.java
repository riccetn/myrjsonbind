package se.narstrom.myr.json.bind.property;

import jakarta.json.stream.JsonGenerator;
import se.narstrom.myr.json.bind.MyrJsonbContext;

public interface PropertySerializer<T> {
	void serializeProperty(final T object, final JsonGenerator generator, final MyrJsonbContext context, final ReadableProperty property);
}
