package se.narstrom.myr.json.bind.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

import jakarta.json.bind.JsonbException;

public final class ReflectionUilities {
	private ReflectionUilities() {
	}

	public static Type getAncestorType(final Type type, final Class<?> rawAncestor) {
		if (type == null)
			return null;

		if (getRawType(type) == rawAncestor)
			return type;

		for (final Type interfaceType : getInterfaces(type)) {
			if (rawAncestor == getRawType(interfaceType))
				return interfaceType;

			final Type superInterface = getAncestorType(interfaceType, rawAncestor);
			if (superInterface != null)
				return superInterface;
		}

		return getAncestorType(getSuperType(type), rawAncestor);
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

	public static Type[] getInterfaces(final Type type) {
		final Class<?> rawType = getRawType(type);
		final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
		final Type[] typeArguments = getTypeArguments(type);

		if (typeArguments.length == 0)
			return rawType.getInterfaces();

		assert typeParameters.length == typeArguments.length;

		final Type[] interfaces = rawType.getGenericInterfaces();

		for (int i = 0; i < interfaces.length; ++i)
			interfaces[i] = resolveType(interfaces[i], typeParameters, typeArguments);

		return interfaces;
	}

	public static Class<?> getRawType(final Type type) {
		if (type instanceof ParameterizedType parameterized) {
			return (Class<?>) parameterized.getRawType();
		} else if (type instanceof Class<?> clazz) {
			return clazz;
		} else if (type instanceof GenericArrayType genericArray) {
			return getRawType(genericArray.getGenericComponentType()).arrayType();
		} else if (type instanceof TypeVariable<?> variable) {
			final Type[] upperBounds = variable.getBounds();
			if (upperBounds.length == 0)
				return Object.class;
			else if (upperBounds.length == 1)
				return getRawType(upperBounds[0]);
			else
				throw new JsonbException("Multiple upper bounds is not supported");
		} else if (type instanceof WildcardType wildcard) {
			final Type[] upperBounds = wildcard.getUpperBounds();
			if (upperBounds.length == 0)
				return Object.class;
			else if (upperBounds.length == 1)
				return getRawType(upperBounds[0]);
			else
				throw new JsonbException("Multiple upper bounds is not supported");
		} else {
			throw new JsonbException("Unsupported type " + type + " (" + type.getClass() + ")");
		}
	}

	public static Type getSuperType(final Type type) {
		final Class<?> rawType = getRawType(type);
		final Type superType = rawType.getGenericSuperclass();

		if (superType == null)
			return null;

		final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
		final Type[] typeArguments = getTypeArguments(type);

		assert typeParameters.length == typeArguments.length;

		return resolveType(superType, typeParameters, typeArguments);
	}

	public static Type[] getTypeArguments(final Type type) {
		if (type instanceof ParameterizedType parameterized) {
			return parameterized.getActualTypeArguments();
		} else if (type instanceof Class<?> clazz) {
			final Type[] arguments = new Type[clazz.getTypeParameters().length];
			Arrays.fill(arguments, Object.class);
			return arguments;
		} else {
			throw new JsonbException("Unsupported type: " + type + ", " + type.getClass().getName());
		}
	}

	public static TypeVariable<?>[] getTypeParameters(final Type type) {
		return getRawType(type).getTypeParameters();
	}

	public static Type resolveType(final Type type, final Type reference) {
		return resolveType(type, getTypeParameters(reference), getTypeArguments(reference));
	}

	public static Type resolveType(final Type type, final TypeVariable<?>[] parameters, final Type[] arguments) {
		if (type instanceof Class<?>) {
			return type;

		} else if (type instanceof GenericArrayType genericArray) {
			final Type componentType = genericArray.getGenericComponentType();
			return new GenericArrayTypeImpl(resolveType(componentType, parameters, arguments));

		} else if (type instanceof ParameterizedType parameterized) {
			final Type[] typeArguments = parameterized.getActualTypeArguments();
			for (int i = 0; i < typeArguments.length; ++i) {
				for (int j = 0; j < parameters.length; ++j) {
					if (typeArguments[i] == parameters[j])
						typeArguments[i] = arguments[j];
				}
			}
			return new ParameterizedTypeImpl(parameterized.getOwnerType(), (Class<?>) parameterized.getRawType(), typeArguments);

		} else if (type instanceof TypeVariable<?> typeVariable) {
			for (int i = 0; i < parameters.length; ++i) {
				if (typeVariable == parameters[i])
					return arguments[i];
			}
			return Object.class;

		} else if (type instanceof WildcardType wildcard) {
			final Type[] bounds = wildcard.getUpperBounds();
			if (bounds.length == 0)
				return Object.class;
			else if (bounds.length == 1)
				return bounds[0];
			else
				throw new JsonbException("Multiple bounds not supported yet");

		} else {
			throw new JsonbException("Unsupported type: " + type + ", " + type.getClass().getName());
		}
	}
}
