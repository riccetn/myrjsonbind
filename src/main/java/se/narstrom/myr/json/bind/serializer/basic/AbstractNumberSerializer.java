package se.narstrom.myr.json.bind.serializer.basic;

import java.text.DecimalFormat;

import jakarta.json.bind.annotation.JsonbNumberFormat;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import se.narstrom.myr.json.bind.MyrJsonbContext;
import se.narstrom.myr.json.bind.serializer.defaults.prioperty.ReadableProperty;
import se.narstrom.myr.json.bind.serializer.defaults.serializer.PropertySerializer;

public interface AbstractNumberSerializer<T extends Number> extends JsonbSerializer<T>, PropertySerializer<T> {

	@Override
	public default void serialize(final T number, final JsonGenerator generator, final SerializationContext ctx) {
		write(number, generator);
	}

	@Override
	public default void serializeProperty(final T number, final JsonGenerator generator, final MyrJsonbContext context, final ReadableProperty property) {
		final JsonbNumberFormat numberFormatAnnotation = property.getAnnotation(JsonbNumberFormat.class);

		if (numberFormatAnnotation != null) {
			final DecimalFormat format = new DecimalFormat(numberFormatAnnotation.value());
			generator.write(format.format(number));
		} else {
			write(number, generator);
		}
	}

	void write(final T number, final JsonGenerator generator);
}
