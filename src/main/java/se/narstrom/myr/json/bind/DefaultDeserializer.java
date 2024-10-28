package se.narstrom.myr.json.bind;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

public final class DefaultDeserializer implements JsonbDeserializer<Object> {

	@Override
	public Object deserialize(final JsonParser parser, final DeserializationContext ctx, final Type type) {
		final Class<?> clazz = (Class<?>) type;

		if (clazz.isArray())
			return deserializeArray(parser, ctx, clazz);

		if (parser.currentEvent() != Event.START_OBJECT)
			throw new JsonbException("Not an object");

		final Object instance;
		try {
			instance = clazz.getConstructor().newInstance();
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}

		Event event;
		while ((event = parser.next()) != Event.END_OBJECT) {
			assert event == Event.KEY_NAME;
			final String key = parser.getString();

			if (key.isEmpty())
				continue;

			final Method setter = findSetter(clazz, key);
			if (setter != null) {
				final Class<?> propertyType = setter.getParameterTypes()[0];
				final Object value = ctx.deserialize(propertyType, parser);
				try {
					setter.invoke(instance, value);
				} catch (final ReflectiveOperationException ex) {
					throw new JsonbException(ex.getMessage(), ex);
				}
			} else {
				Field field;
				try {
					field = clazz.getField(key);
				} catch (final NoSuchFieldException ex) {
					field = null;
				}

				if (field != null) {
					final Class<?> propertyType = field.getType();
					final Object value = ctx.deserialize(propertyType, parser);
					try {
						field.set(propertyType, value);
					} catch (final ReflectiveOperationException ex) {
						throw new JsonbException(ex.getMessage(), ex);
					}
				} else {
					parser.getValue();
					parser.next();
				}
			}
		}

		return instance;
	}

	private Object deserializeArray(final JsonParser parser, final DeserializationContext ctx, final Class<?> clazz) {
		final Class<?> componentClazz = clazz.getComponentType();
		final List<Object> elements = new ArrayList<>();

		if (parser.currentEvent() != Event.START_ARRAY)
			throw new JsonbException("Not an array: " + parser.currentEvent());

		Event event;
		while ((event = parser.next()) != Event.END_ARRAY) {
			final Object elem = ctx.deserialize(componentClazz, parser);
			elements.add(elem);
		}

		if (componentClazz.isPrimitive()) {
			return listToPrimitiveArray(elements, componentClazz);
		} else {
			return elements.toArray(len -> (Object[]) Array.newInstance(componentClazz, len));
		}
	}

	private Object listToPrimitiveArray(final List<?> list, final Class<?> clazz) {
		if (clazz == Boolean.TYPE) {
			final boolean[] array = new boolean[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (boolean) elem;
			}
			return array;
		} else if (clazz == Byte.TYPE) {
			final byte[] array = new byte[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (byte) elem;
			}
			return array;
		} else if (clazz == Character.TYPE) {
			final char[] array = new char[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (char) elem;
			}
			return array;
		} else if (clazz == Double.TYPE) {
			final double[] array = new double[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (double) elem;
			}
			return array;
		} else if (clazz == Float.TYPE) {
			final float[] array = new float[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (float) elem;
			}
			return array;
		} else if (clazz == Integer.TYPE) {
			final int[] array = new int[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (int) elem;
			}
			return array;
		} else if (clazz == Long.TYPE) {
			final long[] array = new long[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (long) elem;
			}
			return array;
		} else if (clazz == Short.TYPE) {
			final short[] array = new short[list.size()];
			int i = 0;
			for (Object elem : list) {
				array[i++] = (short) elem;
			}
			return array;
		} else {
			throw new AssertionError("Unreachable");
		}
	}

	private Method findSetter(final Class<?> clazz, final String key) {
		final int firstCodePoint = key.codePointAt(0);
		if (!Character.isJavaIdentifierStart(firstCodePoint))
			return null;

		final String setterName = "set" + Character.toString(Character.toUpperCase(firstCodePoint)) + key.substring(1);

		for (Class<?> currentClazz = clazz; currentClazz != null && currentClazz != Object.class; currentClazz = currentClazz.getSuperclass()) {
			for (final Method method : currentClazz.getMethods()) {

				if (!Modifier.isPublic(method.getModifiers()))
					continue;

				if (method.isBridge())
					continue;

				if (!Objects.equals(method.getName(), setterName))
					continue;

				if (method.getParameterCount() != 1)
					continue;

				return method;
			}
		}

		return null;
	}
}
