package de.soderer.utilities;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Data formats of import and export files.
 */
public enum FileDataType {
	/** Comma separated values (CSV) */
	CSV("csv"),
	/** Microsoft Excel spreadsheet (xls, xlsx) */
	EXCEL("xlsx", "xls"),
	/** JSON data */
	JSON("json"),
	/** YAML data */
	YAML("yaml", "yml"),
	/** KeePass database (kdbx) */
	KDBX("kdbx"),
	/** OpenDocument spreadsheet (ods) */
	ODS("ods"),
	/** SQL statements */
	SQL("sql"),
	/** vCard data (vcf) */
	VCF("vcf"),
	/** XML data */
	XML("xml");

	/** File extensions without leading dot, the first one is the default */
	private final String[] fileExtensions;

	FileDataType(final String... fileExtensions) {
		this.fileExtensions = fileExtensions;
	}

	/**
	 * Returns the default file extension of this data format.
	 *
	 * @return the file extension without leading dot, like "csv" or "xlsx"
	 */
	public String getDefaultFileExtension() {
		return fileExtensions[0];
	}

	/**
	 * Returns the data type for a name or a file extension, ignoring case and surrounding whitespace.
	 *
	 * @param dataTypeString
	 *            the name of the data type, like "csv", or one of its file extensions with or without leading dot, like
	 *            "yml" or ".xls"
	 * @return the matching data type
	 * @throws IllegalArgumentException
	 *             if there is no data type with this name or extension
	 */
	public static FileDataType getFromString(final String dataTypeString) {
		if (dataTypeString != null) {
			String searchValue = dataTypeString.trim();
			if (searchValue.startsWith(".")) {
				searchValue = searchValue.substring(1);
			}
			for (final FileDataType dataType : FileDataType.values()) {
				// name() instead of toString(), so an overridden toString() cannot change the accepted names
				if (dataType.name().equalsIgnoreCase(searchValue)) {
					return dataType;
				}
				for (final String fileExtension : dataType.fileExtensions) {
					if (fileExtension.equalsIgnoreCase(searchValue)) {
						return dataType;
					}
				}
			}
		}
		throw new IllegalArgumentException("Invalid file data type: '" + dataTypeString + "' (valid: "
				+ Arrays.stream(values()).map(FileDataType::name).collect(Collectors.joining(", ")) + ")");
	}
}
