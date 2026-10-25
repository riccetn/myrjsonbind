package se.narstrom.myr.json.bind.property;

public sealed interface WriteableProperty extends Property permits FieldProperty, SetterProperty {
	void setValue(final Object bean, final Object value);
}
