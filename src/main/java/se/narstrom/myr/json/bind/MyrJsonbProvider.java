package se.narstrom.myr.json.bind;

import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.spi.JsonbProvider;

public final class MyrJsonbProvider extends JsonbProvider {

	@Override
	public JsonbBuilder create() {
		return new MyrJsonbBuilder();
	}
}
