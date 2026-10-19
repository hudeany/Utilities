package de.soderer.utilities;

import java.util.Objects;

/**
 * Type of a data file, consisting of its data format and its optional compression.
 */
public class FileType {
	private FileDataType fileDataType = null;
	private FileCompressionType fileCompressionType = null;

	/**
	 * Creates a file type without data type and without compression.
	 */
	public FileType() {
		// Nothing to do
	}

	/**
	 * Creates a file type.
	 *
	 * @param fileDataType
	 *            the data format, or null if unknown
	 * @param fileCompressionType
	 *            the compression, or null if the file is not compressed
	 */
	public FileType(final FileDataType fileDataType, final FileCompressionType fileCompressionType) {
		this.fileDataType = fileDataType;
		this.fileCompressionType = fileCompressionType;
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
	 * Sets the data format.
	 *
	 * @param newFileDataType
	 *            the data format, or null if unknown
	 * @return this instance for chaining
	 */
	public FileType withFileDataType(final FileDataType newFileDataType) {
		setFileDataType(newFileDataType);
		return this;
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

	/**
	 * Sets the compression.
	 *
	 * @param newFileCompressionType
	 *            the compression, or null if the file is not compressed
	 * @return this instance for chaining
	 */
	public FileType withFileCompressionType(final FileCompressionType newFileCompressionType) {
		setFileCompressionType(newFileCompressionType);
		return this;
	}

	/**
	 * Two file types are equal, if they have the same data format and the same compression.
	 */
	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof FileType)) {
			return false;
		} else {
			final FileType otherFileType = (FileType) other;
			return fileDataType == otherFileType.fileDataType && fileCompressionType == otherFileType.fileCompressionType;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(fileDataType, fileCompressionType);
	}

	/**
	 * Returns a text like "CSV" or "CSV (ZIP)" for logging and error messages.
	 */
	@Override
	public String toString() {
		return (fileDataType == null ? "unknown" : fileDataType.name()) + (fileCompressionType == null ? "" : " (" + fileCompressionType.name() + ")");
	}
}
