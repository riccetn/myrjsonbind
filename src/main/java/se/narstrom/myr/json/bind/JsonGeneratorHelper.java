package se.narstrom.myr.json.bind;

import java.math.BigDecimal;
import java.math.BigInteger;

import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonGenerator;

public final class JsonGeneratorHelper implements JsonGenerator {
	private final JsonGenerator generator;
	private String propertyName;

	public JsonGeneratorHelper(final JsonGenerator generator) {
		this.generator = generator;
	}

	public void setNextPropertyName(final String name) {
		this.propertyName = name;
	}

	public void clearNextPropertyName() {
		this.propertyName = null;
	}

	@Override
	public void close() {
		propertyName = null;
		generator.close();
	}

	@Override
	public void flush() {
		generator.flush();
	}

	@Override
	public JsonGenerator write(final BigDecimal value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		propertyName = null;
		return this;
	}

	@Override
	public JsonGenerator write(final BigInteger value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		propertyName = null;
		return this;
	}

	@Override
	public JsonGenerator write(final boolean value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		return this;
	}

	@Override
	public JsonGenerator write(final double value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		return this;
	}

	@Override
	public JsonGenerator write(final int value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		return this;
	}

	@Override
	public JsonGenerator write(final JsonValue value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		return this;
	}

	@Override
	public JsonGenerator write(final long value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		return this;
	}

	@Override
	public JsonGenerator write(final String value) {
		if (propertyName != null)
			generator.write(propertyName, value);
		else
			generator.write(value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final BigDecimal value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final BigInteger value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final boolean value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final double value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final int value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final JsonValue value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final long value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator write(final String name, final String value) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.write(name, value);
		return this;
	}

	@Override
	public JsonGenerator writeEnd() {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.writeEnd();
		return this;
	}

	@Override
	public JsonGenerator writeKey(final String name) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.writeKey(name);
		return this;
	}

	@Override
	public JsonGenerator writeNull() {
		if (propertyName != null)
			clearNextPropertyName();
		else
			generator.writeNull();
		return this;
	}

	@Override
	public JsonGenerator writeNull(final String name) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.writeNull(name);
		return this;
	}

	@Override
	public JsonGenerator writeStartArray() {
		if (propertyName != null)
			generator.writeStartArray(propertyName);
		else
			generator.writeStartArray();
		propertyName = null;
		return this;
	}

	@Override
	public JsonGenerator writeStartArray(final String name) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.writeStartArray(name);
		return this;
	}

	@Override
	public JsonGenerator writeStartObject() {
		if (propertyName != null)
			generator.writeStartObject(propertyName);
		else
			generator.writeStartObject();
		propertyName = null;
		return this;
	}

	@Override
	public JsonGenerator writeStartObject(final String name) {
		if (propertyName != null)
			throw new JsonException("Not expecting a property name");
		generator.writeStartObject(name);
		return this;
	}
}
