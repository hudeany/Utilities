package de.soderer.utilities.db;

public enum SimpleDataType {
	Integer, // -2.147.483.648 (-2^31) to 2.147.483.647 (-2^31-1)
	BigInteger, // -9.223.372.036.854.775.808 (-2^63) to 9.223.372.036.854.775.807 (2^63-1)
	Float, // Having a precision of releveant dibits and scale of exponent
	Date,
	DateTime,
	String,
	Clob,
	Blob;

	public static SimpleDataType getSimpleDataTypeByName(final String name) {
		for (final SimpleDataType simpleDataType : SimpleDataType.values()) {
			if (simpleDataType.name().equalsIgnoreCase(name)) {
				return simpleDataType;
			}
		}
		throw new RuntimeException("Unknown SimpleDataType: " + name);
	}
}
