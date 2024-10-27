package se.narstrom.myr.json.bind;

import java.util.Objects;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.spi.JsonProvider;

public final class MyrJsonbBuilder implements JsonbBuilder {

	private JsonbConfig config;

	private JsonProvider jsonp;

	@Override
	public JsonbBuilder withConfig(final JsonbConfig config) {
		this.config = Objects.requireNonNull(config);
		return this;
	}

	@Override
	public JsonbBuilder withProvider(final JsonProvider jsonp) {
		this.jsonp = Objects.requireNonNull(jsonp);
		return this;
	}

	@Override
	public Jsonb build() {
		if(jsonp == null)
			jsonp = JsonProvider.provider();
		return new MyrJsonb(config, jsonp);
	}

}
