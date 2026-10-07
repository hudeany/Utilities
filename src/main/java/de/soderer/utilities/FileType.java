package de.soderer.utilities;

/**
 * Type of a data file, consisting of its data format and its optional compression.
 */
public class FileType {
	FileDataType fileDataType = null;
	FileCompressionType fileCompressionType = null;

	/**
	 * Creates a file type without data type and without compression.
	 */
	public FileType() {
		// Nothing to do
	}

	/**
	 * Returns the data format.
	 *
	 * @return the data format, or null if unknown
	 */
	public FileDataType getFileDataType() {
		return fileDataType;
	}

	/**
	 * Sets the data format.
	 *
	 * @param fileDataType
	 *            the data format, or null if unknown
	 */
	public void setFileDataType(final FileDataType fileDataType) {
		this.fileDataType = fileDataType;
	}

	/**
	 * Returns the compression.
	 *
	 * @return the compression, or null if the file is not compressed
	 */
	public FileCompressionType getFileCompressionType() {
		return fileCompressionType;
	}

	/**
	 * Sets the compression.
	 *
	 * @param fileCompressionType
	 *            the compression, or null if the file is not compressed
	 */
	public void setFileCompressionType(final FileCompressionType fileCompressionType) {
		this.fileCompressionType = fileCompressionType;
	}
}
