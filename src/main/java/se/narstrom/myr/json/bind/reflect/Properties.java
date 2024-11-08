package se.narstrom.myr.json.bind.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbProperty;

public final class Properties {

	public static SequencedMap<String, Property> getProperties(final Type beanType) {
		final Class<?> rawType = ReflectionUilities.getRawType(beanType);

		if (rawType.isAnnotation() || rawType.isArray() || rawType.isEnum() || rawType.isInterface() || rawType.isPrimitive() || rawType.isSynthetic())
			throw new JsonbException("Cannot list properties of type " + beanType.getTypeName());

		if (rawType == Object.class)
			return new LinkedHashMap<>();

		final SequencedMap<String, Property> superProperties = getProperties(ReflectionUilities.getSuperType(beanType));

		final Set<String> blacklist = new HashSet<>();
		blacklist.addAll(superProperties.keySet());

		final SortedMap<String, Property> localProperties = new TreeMap<>();

		for (final Field field : rawType.getDeclaredFields()) {

			if (field.isSynthetic())
				continue;

			final JsonbProperty propertyAnnotation = field.getAnnotation(JsonbProperty.class);

			final String name;
			if (propertyAnnotation != null && !propertyAnnotation.value().isEmpty())
				name = propertyAnnotation.value();
			else
				name = field.getName();

			if (blacklist.contains(name))
				continue;

			if (localProperties.containsKey(name))
				throw new JsonbException("Duplicate property name");

			final int fieldModifiers = field.getModifiers();
			if (Modifier.isStatic(fieldModifiers) || Modifier.isTransient(fieldModifiers)) {
				blacklist.add(field.getName());
				continue;
			}

			final Type unresolvedPropertyType = field.getGenericType();
			final Type propertyType = ReflectionUilities.resolveType(unresolvedPropertyType, beanType);

			final Method getter = getGetter(rawType, name, unresolvedPropertyType);
			final Method setter = getSetter(rawType, name, unresolvedPropertyType);

			if (getter != null || setter != null) {
				final boolean publicGetter = getter != null && Modifier.isPublic(getter.getModifiers());
				final boolean publicSetter = setter != null && Modifier.isPublic(setter.getModifiers());

				if (!publicGetter && !publicSetter) {
					blacklist.add(name);
					continue;
				}

				localProperties.put(name, new Property(propertyType, name, null, getter, setter));
			} else {
				if (!Modifier.isPublic(fieldModifiers)) {
					blacklist.add(name);
					continue;
				}

				localProperties.put(name, new Property(propertyType, name, field, null, null));
			}
		}

		for (final Method method : rawType.getDeclaredMethods()) {

			if (method.isBridge() || method.isSynthetic())
				continue;

			final int modifiers = method.getModifiers();
			if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers))
				continue;

			final String methodName = method.getName();
			if (!methodName.startsWith("get") && !methodName.startsWith("set"))
				continue;

			final int firstCodePoint = methodName.codePointAt(3);
			final String remainingName = (firstCodePoint > 0xFFFF) ? methodName.substring(5) : methodName.substring(4);
			final String name = Character.toString(Character.toLowerCase(firstCodePoint)) + remainingName;

			if (blacklist.contains(name) || localProperties.containsKey(name))
				continue;

			final Type unresolvedPropertyType;
			final Method getter;
			final Method setter;

			if (methodName.startsWith("get")) {
				if (method.getParameterCount() != 0)
					continue;

				unresolvedPropertyType = method.getGenericReturnType();
				getter = method;
				setter = getSetter(rawType, name, unresolvedPropertyType);
			} else {
				assert methodName.startsWith("set");

				if (method.getParameterCount() != 1)
					continue;

				unresolvedPropertyType = method.getGenericParameterTypes()[0];
				getter = getGetter(rawType, name, unresolvedPropertyType);
				setter = method;
			}

			final Type propertyType = ReflectionUilities.resolveType(unresolvedPropertyType, beanType);

			localProperties.put(name, new Property(propertyType, name, null, getter, setter));
		}

		final SequencedMap<String, Property> properties = new LinkedHashMap<>();
		properties.putAll(superProperties);
		properties.putAll(localProperties);

		return properties;
	}

	private static Method getGetter(final Class<?> rawType, final String propertyName, final Type propertyType) {
		final int firstCodePoint = propertyName.codePointAt(0);
		final String remainingName = (firstCodePoint > 0xFFFF) ? propertyName.substring(2) : propertyName.substring(1);
		final String getterName = "get" + Character.toString(Character.toUpperCase(firstCodePoint)) + remainingName;

		final Method candidate = getMethod(rawType, getterName);

		if (candidate == null || !candidate.getGenericReturnType().equals(propertyType))
			return null;

		return candidate;
	}

	private static Method getSetter(final Class<?> rawType, final String propertyName, final Type propertyType) {
		final int firstCodePoint = propertyName.codePointAt(0);
		final String remainingName = (firstCodePoint > 0xFFFF) ? propertyName.substring(2) : propertyName.substring(1);
		final String getterName = "set" + Character.toString(Character.toUpperCase(firstCodePoint)) + remainingName;

		return getMethod(rawType, getterName, propertyType);
	}

	private static Method getMethod(final Class<?> rawType, final String methodName, final Type... parameters) {
		try {
			final Class<?>[] rawParameters = new Class[parameters.length];
			for (int i = 0; i < parameters.length; ++i)
				rawParameters[i] = ReflectionUilities.getRawType(parameters[i]);

			final Method method = rawType.getDeclaredMethod(methodName, rawParameters);

			if (method.isBridge() || method.isSynthetic())
				return null;

			return method;

		} catch (final NoSuchMethodException ex) {
			return null;
		}
	}
}
