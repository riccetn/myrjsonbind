package se.narstrom.myr.json.bind.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbProperty;

public final class Properties {

	private static Map<String, Field> listFields(final Class<?> rawBeanType) {
		// @formatter:off
		return Stream.of(rawBeanType.getDeclaredFields())
				.filter(field -> !field.isSynthetic())
				.collect(Collectors.toUnmodifiableMap(Field::getName, Function.identity()));
		// @formatter:on
	}

	private static void listGettersAndSetters(final Class<?> rawBeanType, final Map<String, Method> getters, final Map<String, Method> setters) {
		for (final Method method : rawBeanType.getDeclaredMethods()) {
			if (isGetter(method)) {
				final String name = beanPropertyNameFromMethodName(method.getName());
				final Method oldMethod;
				if ((oldMethod = getters.putIfAbsent(name, method)) != null)
					throw new JsonbException("Multiple getters with the same name " + oldMethod + " vs " + method);
			} else if (isSetter(method)) {
				final String name = beanPropertyNameFromMethodName(method.getName());
				final Method oldMethod;
				if ((oldMethod = setters.putIfAbsent(name, method)) != null)
					throw new JsonbException("Multiple setters with the same name " + oldMethod + " vs " + method);
			}
		}
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

	private static boolean isSetter(final Method method) {
		// @formatter:off
		return method.getName().startsWith("set") &&
				Character.isUpperCase(method.getName().codePointAt(3)) &&
				!method.isSynthetic() && !method.isBridge() &&
				method.getParameterCount() == 1;
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

		final Map<String, Field> fields = listFields(rawType);
		final Map<String, Method> getters = new HashMap<>();
		final Map<String, Method> setters = new HashMap<>();
		listGettersAndSetters(rawType, getters, setters);

		for (final Field field : fields.values()) {

			final String fieldName = field.getName();

			final JsonbProperty propertyAnnotation = field.getAnnotation(JsonbProperty.class);

			final String propertyName;
			if (propertyAnnotation != null && !propertyAnnotation.value().isEmpty())
				propertyName = propertyAnnotation.value();
			else
				propertyName = fieldName;

			if (blacklist.contains(propertyName))
				continue;

			if (localProperties.containsKey(propertyName))
				throw new JsonbException("Duplicate property name");

			final int fieldModifiers = field.getModifiers();
			if (Modifier.isStatic(fieldModifiers) || Modifier.isTransient(fieldModifiers)) {
				blacklist.add(propertyName);
				continue;
			}

			final Type unresolvedPropertyType = field.getGenericType();
			final Type propertyType = ReflectionUilities.resolveType(unresolvedPropertyType, beanType);

			final Method getter = getters.remove(fieldName);
			final Method setter = setters.remove(fieldName);

			if (getter != null || setter != null) {
				final boolean publicGetter = getter != null && Modifier.isPublic(getter.getModifiers());
				final boolean publicSetter = setter != null && Modifier.isPublic(setter.getModifiers());

				if (!publicGetter && !publicSetter) {
					blacklist.add(propertyName);
					continue;
				}

				localProperties.put(propertyName, new Property(propertyType, propertyName, null, getter, setter));
			} else {
				if (!Modifier.isPublic(fieldModifiers)) {
					blacklist.add(propertyName);
					continue;
				}

				localProperties.put(propertyName, new Property(propertyType, propertyName, field, null, null));
			}
		}

		for (final Method getter : getters.values()) {

			final int modifiers = getter.getModifiers();
			if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers))
				continue;

			final String methodName = getter.getName();
			if (!methodName.startsWith("get") && !methodName.startsWith("set"))
				continue;

			final int firstCodePoint = methodName.codePointAt(3);
			final String remainingName = (firstCodePoint > 0xFFFF) ? methodName.substring(5) : methodName.substring(4);
			final String name = Character.toString(Character.toLowerCase(firstCodePoint)) + remainingName;

			if (blacklist.contains(name) || localProperties.containsKey(name))
				continue;

			final Type unresolvedPropertyType = getter.getGenericReturnType();
			final Method setter = setters.remove(name);

			final Type propertyType = ReflectionUilities.resolveType(unresolvedPropertyType, beanType);

			localProperties.put(name, new Property(propertyType, name, null, getter, setter));
		}

		final SequencedMap<String, Property> properties = new LinkedHashMap<>();
		properties.putAll(superProperties);
		properties.putAll(localProperties);

		return properties;
	}
}
