package se.narstrom.myr.json.bind.reflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

public class ParameterizedTypeImpl implements ParameterizedType {
	private final Type ownerType;
	private final Class<?> rawType;
	private final Type[] arguments;

	public ParameterizedTypeImpl(final Type ownerType, final Class<?> rawType, final Type[] arguments) {
		this.ownerType = ownerType;
		this.rawType = rawType;
		this.arguments = arguments.clone();
	}

	@Override
	public Type[] getActualTypeArguments() {
		return arguments.clone();
	}

	@Override
	public Type getRawType() {
		return rawType;
	}

	@Override
	public Type getOwnerType() {
		return ownerType;
	}

	@Override
	public final boolean equals(final Object otherObject) {
		if (!(otherObject instanceof ParameterizedType other))
			return false;
		return Objects.equals(arguments, other.getActualTypeArguments()) && Objects.equals(rawType, other.getRawType()) && Objects.equals(ownerType, other.getOwnerType());
	}

	@Override
	public final int hashCode() {
		return Objects.hashCode(ownerType) ^ Objects.hashCode(rawType) ^ Arrays.hashCode(arguments);
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder();
		if (ownerType != null)
			sb.append(ownerType.getTypeName() + ".");

		sb.append(rawType.getSimpleName());
		if (arguments != null && arguments.length >= 1) {
			sb.append("<" + arguments[0].getTypeName());
			for (int i = 1; i < arguments.length; ++i) {
				sb.append(", " + arguments[i].getTypeName());
			}
			sb.append(">");
		}
		return sb.toString();
	}
}
