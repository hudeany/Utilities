package de.soderer.utilities.db;

import de.soderer.utilities.collection.MultiValueCaseInsensitiveOrderedMap;

public class OracleTnsMapValue implements OracleTnsValue {
	private final MultiValueCaseInsensitiveOrderedMap<OracleTnsValue> tnsValues;

	public OracleTnsMapValue(final MultiValueCaseInsensitiveOrderedMap<OracleTnsValue> tnsValues) {
		this.tnsValues = tnsValues;
	}

	@Override
	public MultiValueCaseInsensitiveOrderedMap<OracleTnsValue> getValue() {
		return tnsValues;
	}
}
