package se.narstrom.myr.json.bind.serializer;

import java.util.SequencedMap;

import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import se.narstrom.myr.json.bind.JsonGeneratorHelper;
import se.narstrom.myr.json.bind.MyrJsonbContext;
import se.narstrom.myr.json.bind.reflect.Properties;
import se.narstrom.myr.json.bind.reflect.Property;

public final class DefaultSerializer implements JsonbSerializer<Object> {

	@Override
	public void serialize(final Object object, final JsonGenerator generator, final SerializationContext context) {
		final Class<?> clazz = object.getClass();

		assert !clazz.isArray() && !clazz.isPrimitive();

		generator.writeStartObject();
		serializeProperties(object, clazz, generator, context);
		generator.writeEnd();
	}

	private void serializeProperties(final Object object, final Class<?> clazz, final JsonGenerator generator, final SerializationContext context) {

		final SequencedMap<String, Property> properties = Properties.getProperties(clazz);

		final boolean writeNulls = getWriteNulls(context);

		for (final Property property : properties.values()) {
			final Object value = property.getValue(object);

			if (writeNulls) {
				context.serialize(property.name(), value, generator);
			} else {
				final JsonGeneratorHelper helper;
				if (generator instanceof JsonGeneratorHelper)
					helper = (JsonGeneratorHelper) generator;
				else
					helper = new JsonGeneratorHelper(generator);

				helper.setNextPropertyName(property.name());
				context.serialize(value, helper);
				helper.clearNextPropertyName();
			}
		}
	}

	private boolean getWriteNulls(final SerializationContext context) {
		return ((MyrJsonbContext) context).getConfig().getProperty(JsonbConfig.NULL_VALUES).map(Boolean.class::cast).orElse(Boolean.FALSE);
	}
}
