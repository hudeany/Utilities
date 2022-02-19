package de.soderer.utilities.json.compare;

public class JsonCompareSettings {
	/**
	 * - allow different order of items in arrays
	 */
	private boolean allowArrayMixedOrder = false;

	public boolean isAllowArrayMixedOrder() {
		return allowArrayMixedOrder;
	}

	public JsonCompareSettings setAllowArrayMixedOrder(final boolean allowArrayMixedOrder) {
		this.allowArrayMixedOrder = allowArrayMixedOrder;
		return this;
	}
}
