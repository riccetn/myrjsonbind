package se.narstrom.myr.json.bind;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import jakarta.json.bind.JsonbException;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

public final class DefaultSerializer implements JsonbSerializer<Object> {

	@Override
	public void serialize(final Object obj, final JsonGenerator generator, final SerializationContext ctx) {
		generator.writeStartObject();
		final Set<String> writtenProperties = new HashSet<>();

		for (final Method method : obj.getClass().getMethods()) {

			if (method.getParameterCount() != 0)
				continue;

			if(method.isBridge())
				continue;

			final String methodName = method.getName();
			if (methodName.equals("getClass"))
				continue;

			if (!methodName.startsWith("get"))
				continue;

			final int firstCodePoint = methodName.codePointAt(3);
			if (!Character.isUpperCase(firstCodePoint))
				continue;

			final String propertyName;
			if (firstCodePoint > 0xFFFF)
				propertyName = Character.toString(Character.toLowerCase(firstCodePoint)) + methodName.substring(5);
			else
				propertyName = Character.toString(Character.toLowerCase(firstCodePoint)) + methodName.substring(4);

			final Object value;
			try {
				value = method.invoke(obj);
			} catch (final ReflectiveOperationException ex) {
				throw new JsonbException(ex.getMessage(), ex);
			}

			System.out.println("Method property: " + method);
			System.out.println("Property name: " + propertyName);

			ctx.serialize(propertyName, value, generator);
			writtenProperties.add(propertyName);
		}

		for (final Field field : obj.getClass().getFields()) {
			final String fieldName = field.getName();

			if (writtenProperties.contains(fieldName))
				continue;

			final Object value;
			try {
				value = field.get(obj);
			} catch (final ReflectiveOperationException ex) {
				throw new JsonbException(ex.getMessage(), ex);
			}

			System.out.println("Field property " + field);

			ctx.serialize(fieldName, value, generator);
		}

		generator.writeEnd();
	}

}
