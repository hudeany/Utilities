package de.soderer.utilities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hash value of a file or a directory tree.
 * <p>
 * The hash of a file is set from outside, the hash of a directory is calculated from the names and
 * hashes of its sub items (in the order they were added) and cached until another sub item is added.
 * </p>
 */
public class HashItem {
	private final String name;
	private long size;
	/** True if the size of this directory is calculated from its sub items */
	private final boolean sizeIsCalculated;
	private final boolean isDirectory;
	private final HashAlgorithm hashAlgorithm;
	private final List<HashItem> subItems = new ArrayList<>();
	private byte[] hash = null;

	private LocalDateTime creationDate = null;
	private LocalDateTime modifiedDate = null;

	/**
	 * Creates a new hash item without date information.
	 *
	 * @param name
	 *            the file or directory name
	 * @param isDirectory
	 *            true for a directory
	 * @param hashAlgorithm
	 *            the algorithm used to calculate a directory hash
	 * @param size
	 *            the size in bytes of a file, or a negative value for a directory to calculate the size
	 *            from its sub items
	 */
	public HashItem(final String name, final boolean isDirectory, final HashAlgorithm hashAlgorithm, final long size) {
		this.name = name;
		this.isDirectory = isDirectory;
		this.hashAlgorithm = hashAlgorithm;
		this.size = size;
		sizeIsCalculated = isDirectory && size < 0;
	}

	/**
	 * Creates a new hash item with date information.
	 *
	 * @param name
	 *            the file or directory name
	 * @param isDirectory
	 *            true for a directory
	 * @param hashAlgorithm
	 *            the algorithm used to calculate a directory hash
	 * @param size
	 *            the size in bytes of a file, or a negative value for a directory to calculate the size
	 *            from its sub items
	 * @param creationDate
	 *            the creation date, may be null
	 * @param modifiedDate
	 *            the last modification date, may be null
	 */
	public HashItem(final String name, final boolean isDirectory, final HashAlgorithm hashAlgorithm, final long size, final LocalDateTime creationDate, final LocalDateTime modifiedDate) {
		this.name = name;
		this.isDirectory = isDirectory;
		this.hashAlgorithm = hashAlgorithm;
		this.size = size;
		sizeIsCalculated = isDirectory && size < 0;

		this.creationDate = creationDate;
		this.modifiedDate = modifiedDate;
	}

	/**
	 * Returns the hash. For a directory it is calculated from the names and hashes of all sub items.
	 *
	 * @return the hash, or null if it was not set yet (for a directory: if any sub item has no hash)
	 * @throws Exception
	 *             if the hash algorithm is not available
	 */
	public byte[] getHash() throws Exception {
		if (isDirectory && hash == null) {
			final StringBuilder directoryContentData = new StringBuilder();
			for (final HashItem item : subItems) {
				directoryContentData.append(item.getName());
				if (item.getHash() == null) {
					return null;
				} else {
					directoryContentData.append(BitUtilities.toHexString(item.getHash()));
					directoryContentData.append("\n");
				}
			}

			final MessageDigest messageDigest = hashAlgorithm.getMessageDigest();
			hash = messageDigest.digest(directoryContentData.toString().getBytes(StandardCharsets.UTF_8));
		}

		return hash;
	}

	/**
	 * Sets the hash of a file.
	 *
	 * @param hash
	 *            the hash value
	 * @throws Exception
	 *             if this item is a directory, whose hash is always calculated
	 */
	public void setHash(final byte[] hash) throws Exception {
		if (isDirectory) {
			throw new Exception("Directory hash may not be set, but must be calculated");
		} else {
			this.hash = hash;
		}
	}

	/**
	 * Returns the name.
	 *
	 * @return the file or directory name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns whether this item is a directory.
	 *
	 * @return true for a directory
	 */
	public boolean isDirectory() {
		return isDirectory;
	}

	/**
	 * Adds a sub item to this directory item and resets the cached directory hash.
	 *
	 * @param subItem
	 *            the sub item to add
	 */
	public void addSubItem(final HashItem subItem) {
		hash = null;
		if (sizeIsCalculated) {
			// Invalidate a cached calculated size
			size = -1;
		}
		subItems.add(subItem);
	}

	/**
	 * Returns the sub items in an unmodifiable list.
	 *
	 * @return a snapshot of the sub items
	 */
	public List<HashItem> getSubItems() {
		return Collections.unmodifiableList(new ArrayList<>(subItems));
	}

	/**
	 * Returns the size in bytes. The size of a directory created with a negative size is calculated as
	 * sum of all sub items on the first call and cached afterwards.
	 *
	 * @return the size in bytes
	 */
	public long getSize() {
		if (isDirectory && size < 0) {
			size = 0;
			for (final HashItem subItem : subItems) {
				size += subItem.getSize();
			}
		}

		return size;
	}

	/**
	 * Returns the number of files.
	 *
	 * @return 1 for a file, the number of all files in the directory tree for a directory
	 */
	public long getNumberOfFiles() {
		if (isDirectory) {
			long filesCount = 0;
			for (final HashItem subItem : subItems) {
				filesCount += subItem.getNumberOfFiles();
			}
			return filesCount;
		} else {
			return 1;
		}
	}

	/**
	 * Returns the number of sub directories.
	 *
	 * @return the number of all sub directories in the directory tree, 0 for a file
	 */
	public long getNumberOfDirectories() {
		if (isDirectory) {
			long directoriesCount = 0;
			for (final HashItem subItem : subItems) {
				if (subItem.isDirectory()) {
					directoriesCount++;
					directoriesCount += subItem.getNumberOfDirectories();
				}
			}
			return directoriesCount;
		} else {
			return 0;
		}
	}

	/**
	 * Returns the creation date of this item.
	 *
	 * @return the creation date, or null if unknown
	 */
	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	/**
	 * Returns the last modification date of this item.
	 *
	 * @return the modification date, or null if unknown
	 */
	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}

	/**
	 * Returns the latest creation date of all files in this item.
	 *
	 * @return the creation date of a file, or the latest creation date of all files in the directory
	 *         tree, or null if unknown
	 */
	public LocalDateTime getLatestFileCreation() {
		if (isDirectory) {
			LocalDateTime latestFileCreationDate = null;
			for (final HashItem subItem : subItems) {
				final LocalDateTime subItemLatestFileCreation = subItem.getLatestFileCreation();
				if (latestFileCreationDate == null) {
					latestFileCreationDate = subItemLatestFileCreation;
				} else if (subItemLatestFileCreation != null && latestFileCreationDate.isBefore(subItemLatestFileCreation)) {
					latestFileCreationDate = subItemLatestFileCreation;
				}
			}
			return latestFileCreationDate;
		} else {
			return creationDate;
		}
	}

	/**
	 * Returns the latest modification date of all files in this item.
	 *
	 * @return the modification date of a file, or the latest modification date of all files in the
	 *         directory tree, or null if unknown
	 */
	public LocalDateTime getLatestFileModified() {
		if (isDirectory) {
			LocalDateTime latestFileModifiedDate = null;
			for (final HashItem subItem : subItems) {
				final LocalDateTime subItemLatestFileModified = subItem.getLatestFileModified();
				if (latestFileModifiedDate == null) {
					latestFileModifiedDate = subItemLatestFileModified;
				} else if (subItemLatestFileModified != null && latestFileModifiedDate.isBefore(subItemLatestFileModified)) {
					latestFileModifiedDate = subItemLatestFileModified;
				}
			}
			return latestFileModifiedDate;
		} else {
			return modifiedDate;
		}
	}
}
