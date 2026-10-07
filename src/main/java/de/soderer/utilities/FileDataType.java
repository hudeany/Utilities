package de.soderer.utilities;

/**
 * Data formats of import and export files.
 */
public enum FileDataType {
	/** Comma separated values (CSV) */
	CSV,
	/** Microsoft Excel spreadsheet (xls, xlsx) */
	EXCEL,
	/** JSON data */
	JSON,
	/** YAML data */
	YAML,
	/** KeePass database (kdbx) */
	KDBX,
	/** OpenDocument spreadsheet (ods) */
	ODS,
	/** SQL statements */
	SQL,
	/** vCard data (vcf) */
	VCF,
	/** XML data */
	XML;

	/**
	 * Returns the data type for a name, ignoring case.
	 *
	 * @param dataTypeString
	 *            the name of the data type, like "csv"
	 * @return the matching data type
	 * @throws RuntimeException
	 *             if there is no data type with this name
	 */
	public static FileDataType getFromString(final String dataTypeString) {
		for (final FileDataType dataType : FileDataType.values()) {
			if (dataType.toString().equalsIgnoreCase(dataTypeString)) {
				return dataType;
			}
		}
		throw new RuntimeException("Invalid file data type: " + dataTypeString);
	}
}
