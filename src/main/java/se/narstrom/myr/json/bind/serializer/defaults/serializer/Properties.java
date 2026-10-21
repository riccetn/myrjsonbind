package se.narstrom.myr.json.bind.serializer.defaults.serializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbProperty;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public final class Properties {

	private static Map<String, Field> listFields(final Class<?> rawBeanType) {
		// @formatter:off
		return Stream.of(rawBeanType.getDeclaredFields())
				.filter(field -> !field.isSynthetic())
				.collect(Collectors.toUnmodifiableMap(Field::getName, Function.identity()));
		// @formatter:on
	}

	private static Map<String, Method> listGetters(final Class<?> rawBeanType) {
		final Map<String, Method> getters = new HashMap<>();
		for (final Method method : rawBeanType.getDeclaredMethods()) {
			if (isGetter(method)) {
				final String name = beanPropertyNameFromMethodName(method.getName());
				final Method oldMethod;
				if ((oldMethod = getters.putIfAbsent(name, method)) != null)
					throw new JsonbException("Multiple getters with the same name " + oldMethod + " vs " + method);
			}
		}
		return getters;
	}

	private static boolean isGetter(final Method method) {
		// @formatter:off
		return ((method.getName().startsWith("get") && Character.isUpperCase(method.getName().codePointAt(3))) ||
					(method.getName().startsWith("is") && Character.isUpperCase(method.getName().codePointAt(2)))) &&
				!method.isSynthetic() && !method.isBridge() &&
				method.getParameterCount() == 0 &&
				method.getReturnType() != Void.TYPE;
		// @formatter:on
	}

	private static String beanPropertyNameFromMethodName(final String methodName) {
		final int prefixLength;
		if (methodName.startsWith("get") || methodName.startsWith("set"))
			prefixLength = 3;
		else {
			assert methodName.startsWith("is");
			prefixLength = 2;
		}

		final int firstCodePoint = methodName.codePointAt(prefixLength);
		final String rest = (firstCodePoint > 0xFFFF) ? methodName.substring(prefixLength + 2) : methodName.substring(prefixLength + 1);

		return Character.toString(Character.toLowerCase(firstCodePoint)) + rest;
	}

	private static String getPropertyName(final String name, final Field field, final Method getter) {
		final JsonbProperty fieldAnnotation = (field != null) ? field.getAnnotation(JsonbProperty.class) : null;
		final JsonbProperty getterAnnotation = (getter != null) ? getter.getAnnotation(JsonbProperty.class) : null;

		String propertyName = null;
		if (fieldAnnotation != null)
			propertyName = fieldAnnotation.value();

		if (getterAnnotation != null) {
			if (propertyName != null && !Objects.equals(propertyName, getterAnnotation.value()))
				throw new JsonbException("Conflicting annotations for property " + name);
			propertyName = getterAnnotation.value();
		}

		if (propertyName == null)
			return name;

		return propertyName;
	}

	private static Type getUnresolvedPropertyType(final Field field, final Method getter) {
		if (field != null)
			return field.getGenericType();
		else if (getter != null)
			return getter.getGenericReturnType();
		else
			throw new AssertionError("Unreachable");
	}

	public static SequencedMap<String, Property> getProperties(final Type beanType) {
		final Class<?> rawType = ReflectionUtilities.getRawType(beanType);

		if (rawType.isAnnotation() || rawType.isArray() || rawType.isEnum() || rawType.isInterface() || rawType.isPrimitive() || rawType.isSynthetic())
			throw new JsonbException("Cannot list properties of type " + beanType.getTypeName());

		if (rawType == Object.class)
			return new LinkedHashMap<>();

		final SequencedMap<String, Property> superProperties = getProperties(ReflectionUtilities.getSuperType(beanType));

		final Set<String> blacklist = new HashSet<>();
		blacklist.addAll(superProperties.keySet());

		final SortedMap<String, Property> localProperties = new TreeMap<>();

		final Map<String, Field> fields = listFields(rawType);
		final Map<String, Method> getters = listGetters(rawType);

		final Set<String> names = new HashSet<>();
		names.addAll(fields.keySet());
		names.addAll(getters.keySet());

		for (final String fieldName : names) {

			final Field field = fields.get(fieldName);
			final Method getter = getters.get(fieldName);

			final String propertyName = getPropertyName(fieldName, field, getter);

			if (blacklist.contains(propertyName))
				continue;

			if (localProperties.containsKey(propertyName))
				throw new JsonbException("Duplicate property name");

			if (field != null) {
				final int fieldModifiers = field.getModifiers();
				if (Modifier.isStatic(fieldModifiers) || Modifier.isTransient(fieldModifiers)) {
					blacklist.add(propertyName);
					continue;
				}
			}

			if (getter != null) {
				final int modifiers = getter.getModifiers();
				if (Modifier.isStatic(modifiers)) {
					blacklist.add(propertyName);
					continue;
				}
			}

			final Type unresolvedPropertyType = getUnresolvedPropertyType(field, getter);
			final Type propertyType = ReflectionUtilities.resolveType(unresolvedPropertyType, beanType);

			if (getter != null) {
				final boolean publicGetter = getter != null && Modifier.isPublic(getter.getModifiers());

				if (!publicGetter) {
					blacklist.add(propertyName);
					continue;
				}
			} else {
				assert field != null;

				if (!Modifier.isPublic(field.getModifiers())) {
					blacklist.add(propertyName);
					continue;
				}
			}
			localProperties.put(propertyName, new Property(beanType, propertyType, propertyName, field, getter));
		}

		final SequencedMap<String, Property> properties = new LinkedHashMap<>();
		properties.putAll(superProperties);
		properties.putAll(localProperties);

		return properties;
	}
}
