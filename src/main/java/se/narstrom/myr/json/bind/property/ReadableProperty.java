package se.narstrom.myr.json.bind.property;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public record ReadableProperty(Type beanType, Type type, String name, Field field, Method getter) implements Property {

	public Object getValue(final Object bean) {
		try {
			if (getter != null) {
				try {
					getter.setAccessible(true);
					return getter.invoke(bean);
				} finally {
					getter.setAccessible(false);
				}
			} else if (field != null) {
				try {
					field.setAccessible(true);
					return field.get(bean);
				} finally {
					field.setAccessible(false);
				}
			} else {
				throw new JsonbException("property is not readable");
			}
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.getMessage(), ex);
		}
	}

	public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
		A annotation = null;

		if (getter != null)
			annotation = getter.getAnnotation(annotationType);

		if (annotation == null && field != null)
			annotation = field.getAnnotation(annotationType);

		final Class<?> rawBeanType = ReflectionUtilities.getRawType(beanType);
		if (annotation == null)
			annotation = rawBeanType.getAnnotation(annotationType);

		if (annotation == null)
			annotation = rawBeanType.getPackage().getAnnotation(annotationType);

		return annotation;
	}
}
