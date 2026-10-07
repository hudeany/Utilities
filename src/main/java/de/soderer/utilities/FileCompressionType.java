package de.soderer.utilities;

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
	 * Returns the compression format for a name, ignoring case.
	 *
	 * @param compressionTypeString
	 *            the name of the compression format, like "zip" or "targz"
	 * @return the matching compression format
	 * @throws RuntimeException
	 *             if there is no compression format with this name
	 */
	public static FileCompressionType getFromString(final String compressionTypeString) {
		for (final FileCompressionType compressionType : FileCompressionType.values()) {
			if (compressionType.toString().equalsIgnoreCase(compressionTypeString)) {
				return compressionType;
			}
		}
		throw new RuntimeException("Invalid compression type: " + compressionTypeString);
	}
}
