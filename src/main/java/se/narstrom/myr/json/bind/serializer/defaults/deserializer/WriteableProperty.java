package se.narstrom.myr.json.bind.serializer.defaults.deserializer;

public sealed interface WriteableProperty extends Property permits FieldProperty, SetterProperty {
	void set(final Object bean, final Object value);
}
