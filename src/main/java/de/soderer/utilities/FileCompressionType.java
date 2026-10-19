package de.soderer.utilities;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Compression formats of data files.
 */
public enum FileCompressionType {
	/** Zip archive */
	ZIP ("zip"),
	/** Gzip compressed tar archive with extension "tar.gz" */
	TARGZ ("tar.gz"),
	/** Gzip compressed tar archive with extension "tgz" */
	TGZ ("tgz"),
	/** Gzip compressed single file */
	GZ ("gz");

	private final String defaultFileExtension;

	/**
	 * Returns the default file extension of this compression format.
	 *
	 * @return the file extension without leading dot, like "tar.gz"
	 */
	public String getDefaultFileExtension() {
		return defaultFileExtension;
	}

	FileCompressionType(final String defaultFileExtension) {
		this.defaultFileExtension = defaultFileExtension;
	}

	/**
	 * Returns the compression format for a name or a default file extension, ignoring case and surrounding whitespace.
	 *
	 * @param compressionTypeString
	 *            the name of the compression format, like "zip" or "targz", or its default file extension with or
	 *            without leading dot, like "tar.gz" or ".tgz"
	 * @return the matching compression format
	 * @throws IllegalArgumentException
	 *             if there is no compression format with this name or extension
	 */
	public static FileCompressionType getFromString(final String compressionTypeString) {
		if (compressionTypeString != null) {
			String searchValue = compressionTypeString.trim();
			if (searchValue.startsWith(".")) {
				searchValue = searchValue.substring(1);
			}
			for (final FileCompressionType compressionType : FileCompressionType.values()) {
				// name() instead of toString(), so an overridden toString() cannot change the accepted names
				if (compressionType.name().equalsIgnoreCase(searchValue) || compressionType.defaultFileExtension.equalsIgnoreCase(searchValue)) {
					return compressionType;
				}
			}
		}
		throw new IllegalArgumentException("Invalid compression type: '" + compressionTypeString + "' (valid: "
				+ Arrays.stream(values()).map(FileCompressionType::name).collect(Collectors.joining(", ")) + ")");
	}
}
