package se.narstrom.myr.json.bind.serializer;

import java.util.HashSet;
import java.util.SequencedMap;
import java.util.Set;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import se.narstrom.myr.json.bind.reflect.Properties;
import se.narstrom.myr.json.bind.reflect.Property;

public final class DefaultSerializer implements JsonbSerializer<Object> {

	@Override
	public void serialize(final Object object, final JsonGenerator generator, final SerializationContext context) {
		final Class<?> clazz = object.getClass();

		assert !clazz.isArray() && !clazz.isPrimitive();

		generator.writeStartObject();
		serializeProperties(object, clazz, generator, context, new HashSet<>());
		generator.writeEnd();
	}

	private void serializeProperties(final Object object, final Class<?> clazz, final JsonGenerator generator, final SerializationContext context, final Set<String> seen) {

		final SequencedMap<String, Property> properties = Properties.getProperties(clazz);

		for (final Property property : properties.values()) {
			context.serialize(property.name(), property.getValue(object), generator);
		}
	}
}
