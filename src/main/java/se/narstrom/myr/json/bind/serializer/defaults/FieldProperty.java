package se.narstrom.myr.json.bind.serializer.defaults;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public record FieldProperty(Type beanType, Field field) implements WriteableProperty {
	@Override
	public void set(final Object bean, final Object value) {
		field.setAccessible(true);
		try {
			field.set(bean, value);
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.toString(), ex);
		} finally {
			field.setAccessible(false);
		}
	}

	@Override
	public Type type() {
		return ReflectionUtilities.resolveType(field.getGenericType(), beanType());
	}

	@Override
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
		T annotation = field.getAnnotation(annotationClass);

		if (annotation == null)
			annotation = ReflectionUtilities.getRawType(beanType).getAnnotation(annotationClass);

		if (annotation == null)
			annotation = ReflectionUtilities.getRawType(beanType).getPackage().getAnnotation(annotationClass);

		return annotation;
	}
}
