package de.soderer.utilities.db;

public class OracleTnsStringValue implements OracleTnsValue {
	private final String value;

	public OracleTnsStringValue(final String value) {
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}
}
