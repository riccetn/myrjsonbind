package se.narstrom.myr.json.bind.serializer.defaults;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

import jakarta.json.JsonObject;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbSubtype;
import jakarta.json.bind.annotation.JsonbTypeInfo;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;
import se.narstrom.myr.json.bind.MyrJsonbContext;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public class DefaultDeserializer implements JsonbDeserializer<Object> {

	@Override
	public Object deserialize(final JsonParser parser, final DeserializationContext context, final Type type) {
		final Class<?> clazz = ReflectionUtilities.getRawType(type);
		if (clazz.isAnnotation() || clazz.isArray() || clazz.isEnum() || clazz.isPrimitive() || clazz.isSynthetic())
			throw new JsonbException("Attempting to deserialize unsupported class " + clazz);

		if (parser.currentEvent() == Event.VALUE_NULL)
			return null;

		if (parser.currentEvent() != Event.START_OBJECT)
			throw new JsonbException("Not an object, event: " + parser.currentEvent());

		final JsonObject jsonObject = parser.getObject();

		final Type concreteType = findConcreteType(jsonObject, clazz, type);
		final Class<?> concreteClazz = ReflectionUtilities.getRawType(concreteType);

		validatePolymorphicType(concreteClazz);

		final Executable creator = findCreator(concreteClazz);
		final SequencedMap<String, CreatorProperty> creatorProperties = findCreatorProperties(creator);
		final Map<String, Property> properties = findProperties(concreteType, concreteClazz);

		properties.putAll(creatorProperties);

		final JsonProvider jsonp = ((MyrJsonbContext) context).getJsonpProvider();
		final JsonParser objectParser = jsonp.createParserFactory(null).createParser(jsonObject);
		objectParser.next();

		final SequencedMap<String, Object> values = new LinkedHashMap<>();

		Event event;
		while ((event = objectParser.next()) != Event.END_OBJECT) {
			assert event == Event.KEY_NAME;
			final String name = objectParser.getString();

			final Property property = properties.get(name);

			if (property == null) {
				if (((MyrJsonbContext) context).getConfig().getProperty("jsonb.fail-on-unknown-properties").orElse(Boolean.FALSE) == Boolean.TRUE)
					throw new JsonbException("Unknown property: " + name);
				objectParser.getValue();
				objectParser.next();
				continue;
			}

			final Object value = context.deserialize(property.type(), objectParser);
			values.put(name, value);
		}

		try {
			final Object[] creatorArguments = new Object[creator.getParameterCount()];
			for (final Map.Entry<String, CreatorProperty> entry : creatorProperties.entrySet()) {
				final CreatorProperty property = entry.getValue();
				final int index = property.index();
				creatorArguments[index] = values.get(entry.getKey());
			}

			final Object object;
			creator.setAccessible(true);
			try {
				object = switch (creator) {
					case Method method -> method.invoke(null, creatorArguments);
					case Constructor<?> constructor -> constructor.newInstance(creatorArguments);
				};
			} finally {
				creator.setAccessible(false);
			}

			for (final Map.Entry<String, Object> entry : values.entrySet()) {
				final Property property = properties.get(entry.getKey());
				final Object value = entry.getValue();
				switch (property) {
					case FieldProperty fieldProperty -> {
						try {
							fieldProperty.field().setAccessible(true);
							fieldProperty.field().set(object, value);
						} finally {
							fieldProperty.field().setAccessible(false);
						}
					}
					case SetterProperty setterProperty -> {
						try {
							setterProperty.setter().setAccessible(true);
							setterProperty.setter().invoke(object, value);
						} finally {
							setterProperty.setter().setAccessible(false);
						}
					}
					case CreatorProperty _ -> {
					}
				}
			}

			return object;
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException("Reflective operation error", ex);
		}
	}

	private Type findConcreteType(final JsonObject object, final Class<?> clazz, final Type type) {
		final JsonbTypeInfo typeInfo = clazz.getAnnotation(JsonbTypeInfo.class);
		if (typeInfo == null)
			return type;

		final String key = typeInfo.key();
		final String value = object.getString(key);

		Class<?> concreteType = null;
		for (final JsonbSubtype subtype : typeInfo.value()) {
			if (value.equals(subtype.alias())) {
				if (concreteType != null)
					throw new JsonbException("Multiple potential subtypes");
				concreteType = subtype.type();
			}
		}

		if (concreteType == null)
			throw new JsonbException("Not subtype found");

		return findConcreteType(object, concreteType, concreteType);
	}

	private void validatePolymorphicType(final Class<?> type) {
		Class<?> parent = null;

		final Class<?> superClass = type.getSuperclass();
		if (superClass != null && superClass.isAnnotationPresent(JsonbTypeInfo.class))
			parent = superClass;

		for (final Class<?> iface : type.getInterfaces()) {
			if (iface.isAnnotationPresent(JsonbTypeInfo.class)) {
				if (parent != null)
					throw new JsonbException("Invalid polymorphic type, multiple parents");
				parent = iface;
			}
		}
	}

	private Executable findCreator(final Class<?> clazz) {
		Executable creator = null;

		for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
			if (!constructor.isAnnotationPresent(JsonbCreator.class))
				continue;
			if (!Modifier.isPublic(constructor.getModifiers()) && !Modifier.isProtected(constructor.getModifiers()))
				throw new JsonbException("Creator most be public or protected");
			if (creator != null)
				throw new JsonbException("Only one creatior is allowed");
			creator = constructor;
		}

		for (final Method method : clazz.getDeclaredMethods()) {
			if (!method.isAnnotationPresent(JsonbCreator.class))
				continue;
			if (!Modifier.isPublic(method.getModifiers()) && !Modifier.isProtected(method.getModifiers()))
				throw new JsonbException("Creator most be public or protected");
			if (!Modifier.isStatic(method.getModifiers()))
				throw new JsonbException("Creator method most be static");
			if (method.getReturnType() != clazz)
				throw new JsonbException("Creator method most return an instance of contained class");
			if (creator != null)
				throw new JsonbException("Only on creator is allowed");
			creator = method;
		}

		if (creator == null) {
			for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
				if (!Modifier.isPublic(constructor.getModifiers()) && !Modifier.isProtected(constructor.getModifiers()))
					continue;
				if (constructor.getParameterCount() != 0)
					continue;
				creator = constructor;
			}
		}

		if (creator == null)
			throw new JsonbException("No public or protected no-args constructor");

		return creator;
	}

	private SequencedMap<String, CreatorProperty> findCreatorProperties(final Executable creator) {
		final SequencedMap<String, CreatorProperty> properties = new LinkedHashMap<>();

		int index = 0;
		for (final Parameter parameter : creator.getParameters()) {
			final JsonbProperty annotation = parameter.getAnnotation(JsonbProperty.class);

			final String name;
			if (annotation != null)
				name = annotation.value();
			else
				name = parameter.getName();

			final Type type = parameter.getParameterizedType();
			if (properties.put(name, new CreatorProperty(type, index++)) != null)
				throw new JsonbException("Duplicate property name");
		}

		return Collections.unmodifiableSequencedMap(properties);
	}

	private Map<String, Property> findProperties(final Type beanType, final Class<?> beanClazz) {
		final Map<String, Property> properties;

		final Type superType = ReflectionUtilities.getSuperType(beanType);
		final Class<?> superClazz = beanClazz.getSuperclass();
		if (superClazz != null && superClazz != Object.class)
			properties = findProperties(superType, superClazz);
		else
			properties = new HashMap<>();

		final Map<String, Field> fields = new HashMap<>();
		for (final Field field : beanClazz.getDeclaredFields()) {
			if (field.isSynthetic())
				continue;
			fields.put(field.getName(), field);
		}

		final Map<String, Method> setters = new HashMap<>();
		for (final Method method : beanClazz.getDeclaredMethods()) {
			if (method.isSynthetic() || method.isBridge())
				continue;

			if (method.getParameterCount() != 1)
				continue;

			final String methodName = method.getName();
			if (!methodName.startsWith("set") || !Character.isUpperCase(methodName.codePointAt(3)))
				continue;

			final int firstCodePoint = methodName.codePointAt(3);
			final String rest = (firstCodePoint > 0xFFFF) ? methodName.substring(5) : methodName.substring(4);
			final String name = Character.toString(Character.toLowerCase(firstCodePoint)) + rest;

			setters.put(name, method);
		}

		final Set<String> names = new HashSet<>();
		names.addAll(fields.keySet());
		names.addAll(setters.keySet());

		final Map<String, Property> localProperties = new HashMap<>();

		for (final String name : names) {
			final Field field = fields.get(name);
			final Method setter = setters.get(name);
			String propertyName = null;
			Type unresolvedPropertyType = null;

			if (field != null) {
				int modifiers = field.getModifiers();
				if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers))
					continue;

				unresolvedPropertyType = field.getGenericType();

				final JsonbProperty annotation = field.getAnnotation(JsonbProperty.class);
				if (annotation != null)
					propertyName = annotation.value();
			}

			if (setter != null) {
				final int modifiers = setter.getModifiers();
				if (Modifier.isStatic(modifiers))
					continue;

				if (unresolvedPropertyType == null)
					unresolvedPropertyType = setter.getGenericParameterTypes()[0];

				final JsonbProperty annotation = setter.getAnnotation(JsonbProperty.class);
				if (annotation != null) {
					if (propertyName != null && !propertyName.equals(annotation.value()))
						throw new JsonbException("Conflicting annotations for property " + name);
					propertyName = annotation.value();
				}
			}

			final Type propertyType = ReflectionUtilities.resolveType(unresolvedPropertyType, beanType);

			if (propertyName == null)
				propertyName = name;

			final Property property;
			if (setter != null) {
				if (Modifier.isPublic(setter.getModifiers()))
					property = new SetterProperty(propertyType, setter);
				else
					continue;
			} else if (field != null && Modifier.isPublic(field.getModifiers())) {
				property = new FieldProperty(propertyType, field);
			} else {
				continue;
			}

			if (localProperties.containsKey(propertyName))
				throw new JsonbException("Duplicate property name " + propertyName + ": " + properties.get(propertyName) + " vs " + property);

			localProperties.put(propertyName, property);
		}

		properties.putAll(localProperties);

		return properties;
	}

	private static sealed interface Property {
		public Type type();
	}

	private record CreatorProperty(Type type, int index) implements Property {
	}

	private record FieldProperty(Type type, Field field) implements Property {
	}

	private record SetterProperty(Type type, Method setter) implements Property {
	}
}
