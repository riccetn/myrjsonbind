package se.narstrom.myr.json.bind.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import jakarta.json.bind.JsonbException;

public final class ReflectionUilities {
	private ReflectionUilities() {
	}

	public static Class<?> getClass(final Type type) {
		if (type instanceof Class<?> clazz)
			return clazz;
		else if (type instanceof ParameterizedType parameterized)
			return getClass(parameterized.getRawType());
		else if (type instanceof TypeVariable<?> variable) {
			final Type[] upperBounds = variable.getBounds();
			if (upperBounds.length != 0)
				return Object.class; // TODO: Find the nearest common super of all types in upperBounds
			return getClass(upperBounds[0]);
		} else
			throw new JsonbException("Unsupported type " + type);
	}

	public static Type getComponentType(final Type type) {
		if (type instanceof GenericArrayType genericType) {
			return genericType.getGenericComponentType();
		} else if (type instanceof Class<?> clazz) {
			return clazz.getComponentType();
		} else {
			throw new JsonbException("Unsupported array type " + type);
		}
	}

	public static Type getGenericInterfaceType(final Type type, final Class<?> rawInterfaceType) throws ReflectiveOperationException {
		if (type == null)
			return null;

		if (getRawType(type) == rawInterfaceType)
			return type;

		for (final Type interfaceType : getInterfaces(type)) {
			if (rawInterfaceType == getRawType(interfaceType))
				return interfaceType;

			final Type superInterface = getGenericInterfaceType(interfaceType, rawInterfaceType);
			if (superInterface != null)
				return superInterface;
		}

		return getGenericInterfaceType(getSuperType(type), rawInterfaceType);
	}

	public static Type[] getInterfaces(final Type type) throws ReflectiveOperationException {
		final Class<?> rawType = getRawType(type);
		final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
		final Type[] typeArguments = getTypeArguments(type);

		assert typeParameters.length == typeArguments.length;

		final Type[] interfaces = rawType.getGenericInterfaces();

		for (int i = 0; i < interfaces.length; ++i)
			interfaces[i] = resolveTypeParameters(interfaces[i], typeParameters, typeArguments);

		return interfaces;
	}

	public static Class<?> getRawType(final Type type) throws ReflectiveOperationException {
		if (type instanceof ParameterizedType parameterized) {
			return (Class<?>) parameterized.getRawType();
		} else if (type instanceof Class<?> clazz) {
			return clazz;
		} else {
			throw new ReflectiveOperationException("Unsupported type " + type);
		}
	}

	public static Type getSuperType(final Type type) throws ReflectiveOperationException {
		final Class<?> rawType = getRawType(type);
		final Type superType = rawType.getGenericSuperclass();

		if (superType == null)
			return null;

		final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
		final Type[] typeArguments = getTypeArguments(type);

		assert typeParameters.length == typeArguments.length;

		return resolveTypeParameters(superType, typeParameters, typeArguments);
	}

	public static Type[] getTypeArguments(final Type type) throws ReflectiveOperationException {
		if (type instanceof ParameterizedType parameterized) {
			return parameterized.getActualTypeArguments();
		} else {
			return new Type[0];
		}
	}

	public static Type resolveTypeParameters(final Type type, final TypeVariable<?>[] parameters, final Type[] arguments) {
		if (!(type instanceof ParameterizedType parameterized))
			return type;

		final Type[] typeArguments = parameterized.getActualTypeArguments();
		for (int i = 0; i < typeArguments.length; ++i) {
			for (int j = 0; j < parameters.length; ++j) {
				if (typeArguments[i] == parameters[j])
					typeArguments[i] = arguments[j];
			}
		}

		return new ParameterizedTypeImpl(parameterized.getOwnerType(), (Class<?>) parameterized.getRawType(), typeArguments);
	}
}
