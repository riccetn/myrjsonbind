package se.narstrom.myr.json.bind.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Objects;

public final class GenericArrayTypeImpl implements GenericArrayType {

	private final Type componentType;

	public GenericArrayTypeImpl(final Type componentType) {
		this.componentType = componentType;
	}

	@Override
	public Type getGenericComponentType() {
		return componentType;
	}

	@Override
	public String toString() {
		return componentType.getTypeName() + "[]";
	}

	@Override
	public boolean equals(final Object otherObject) {
		if (!(otherObject instanceof GenericArrayType other))
			return false;

		return Objects.equals(other, other.getGenericComponentType());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(componentType);
	}
}
