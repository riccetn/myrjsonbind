package se.narstrom.myr.json.bind.serializer.defaults;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.SequencedMap;
import java.util.Set;

import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import se.narstrom.myr.json.bind.MyrJsonbContext;

public final class DefaultSerializer implements JsonbSerializer<Object> {

	@Override
	public void serialize(final Object object, final JsonGenerator generator, final SerializationContext context) {
		final Class<?> clazz = object.getClass();

		assert !clazz.isArray() && !clazz.isPrimitive();

		Set<String> blacklist = new HashSet<>();

		generator.writeStartObject();
		serializeTypeInfos(object, clazz, generator, context, blacklist);
		serializeProperties(object, clazz, generator, context, blacklist);
		generator.writeEnd();
	}

	private void serializeTypeInfos(final Object object, final Class<?> clazz, final JsonGenerator generator, final SerializationContext context, final Set<String> blacklist) {
		final List<JsonbTypeInfo> infos = getTypeInfos(clazz);

		for (final JsonbTypeInfo info : infos) {
			String value = null;

			if (!blacklist.add(info.key()))
				throw new JsonbException("Name conflict");

			for (final JsonbSubtype subtype : info.value()) {
				final Class<?> type = subtype.type();
				if (type.isAssignableFrom(clazz)) {
					if (value != null)
						throw new JsonbException("Multiple subtypes are compatible with concrete type");
					value = subtype.alias();
				}
			}

			if (value != null)
				generator.write(info.key(), value);
		}
	}

	private void serializeProperties(final Object object, final Class<?> clazz, final JsonGenerator generator, final SerializationContext context, final Set<String> blacklist) {
		final SequencedMap<String, Property> properties = Properties.getProperties(clazz);

		for (final String properyName : properties.keySet()) {
			if (blacklist.contains(properyName))
				throw new JsonbException("Name conflict");
		}

		final boolean writeNulls = getWriteNulls(context);

		for (final Property property : properties.values()) {
			final Object value = property.getValue(object);

			if (!writeNulls && isNullOrEmptyOptional(value))
				continue;

			context.serialize(property.name(), value, generator);
		}
	}

	public List<JsonbTypeInfo> getTypeInfos(final Class<?> clazz) {
		Class<?> parent = null;

		final Class<?> superClazz = clazz.getSuperclass();
		if (superClazz != null) {
			if (superClazz.isAnnotationPresent(JsonbTypeInfo.class))
				parent = superClazz;
		}

		for (final Class<?> iface : clazz.getInterfaces()) {
			if (!iface.isAnnotationPresent(JsonbTypeInfo.class))
				continue;
			if (parent != null)
				throw new JsonbException("Multiple polymorphic parents");
			parent = iface;
		}

		final List<JsonbTypeInfo> typeInfos = new ArrayList<>();
		if (parent != null)
			typeInfos.addAll(getTypeInfos(parent));

		final JsonbTypeInfo typeInfo = clazz.getAnnotation(JsonbTypeInfo.class);
		if (typeInfo != null) {
			for (JsonbSubtype subtype : typeInfo.value()) {
				if (!clazz.isAssignableFrom(subtype.type()))
					throw new JsonbException("Invalid subtype, not a subclass");
			}
			typeInfos.add(typeInfo);
		}

		return typeInfos;
	}

	private boolean getWriteNulls(final SerializationContext context) {
		return ((MyrJsonbContext) context).getConfig().getProperty(JsonbConfig.NULL_VALUES).map(Boolean.class::cast).orElse(Boolean.FALSE);
	}

	private boolean isNullOrEmptyOptional(final Object value) {
		return value == null || (value instanceof Optional opt && opt.isEmpty()) || (value instanceof OptionalDouble opt && opt.isEmpty()) || (value instanceof OptionalInt opt && opt.isEmpty())
				|| (value instanceof OptionalLong opt && opt.isEmpty());
	}
}
