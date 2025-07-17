package se.narstrom.myr.json.bind.serializer.defaults;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public sealed interface Property permits WriteableProperty, CreatorProperty {
	public Type beanType();
	public Type type();
	public <T extends Annotation> T getAnnotation(final Class<T> annotationClass);
}
