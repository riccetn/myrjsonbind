package se.narstrom.myr.json.bind.serializer.defaults;

import java.lang.annotation.Annotation;
import java.lang.reflect.Executable;
import java.lang.reflect.Type;

import se.narstrom.myr.json.bind.reflect.ReflectionUtilities;

public record CreatorProperty(Type beanType, Executable creator, int index) implements Property {
	@Override
	public Type type() {
		return ReflectionUtilities.resolveType(creator.getGenericParameterTypes()[index], beanType());
	}

	@Override
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
		T annotation = creator.getParameters()[index].getAnnotation(annotationClass);

		if (annotation == null)
			annotation = ReflectionUtilities.getRawType(beanType()).getAnnotation(annotationClass);

		if (annotation == null)
			annotation = ReflectionUtilities.getRawType(beanType()).getPackage().getAnnotation(annotationClass);

		return annotation;
	}
}
