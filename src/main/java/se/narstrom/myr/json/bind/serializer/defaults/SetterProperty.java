package se.narstrom.myr.json.bind.serializer.defaults;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import jakarta.json.bind.JsonbException;
import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public record SetterProperty(Type beanType, Method setter) implements WriteableProperty {
	@Override
	public void set(final Object bean, final Object value) {
		setter.setAccessible(true);
		try {
			setter.invoke(bean, value);
		} catch (final ReflectiveOperationException ex) {
			throw new JsonbException(ex.toString(), ex);
		} finally {
			setter.setAccessible(false);
		}
	}

	@Override
	public Type type() {
		return ReflectionUtilities.resolveType(setter.getGenericParameterTypes()[0], beanType());
	}

	@Override
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
		T annotation = setter.getAnnotation(annotationClass);

		if (annotation == null)
			annotation = ReflectionUtilities.getRawType(beanType).getAnnotation(annotationClass);

		if (annotation == null)
			annotation = ReflectionUtilities.getRawType(beanType).getPackage().getAnnotation(annotationClass);

		return annotation;
	}
}
