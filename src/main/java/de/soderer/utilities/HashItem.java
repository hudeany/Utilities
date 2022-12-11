package de.soderer.utilities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HashItem {
	private final String name;
	private long size;
	private final boolean isDirectory;
	private final HashAlgorithm hashAlgorithm;
	private final List<HashItem> subItems = new ArrayList<>();
	private byte[] hash = null;

	private LocalDateTime creationDate = null;
	private LocalDateTime modifiedDate = null;

	public HashItem(final String name, final boolean isDirectory, final HashAlgorithm hashAlgorithm, final long size) {
		this.name = name;
		this.isDirectory = isDirectory;
		this.hashAlgorithm = hashAlgorithm;
		this.size = size;
	}

	public HashItem(final String name, final boolean isDirectory, final HashAlgorithm hashAlgorithm, final long size, final LocalDateTime creationDate, final LocalDateTime modifiedDate) {
		this.name = name;
		this.isDirectory = isDirectory;
		this.hashAlgorithm = hashAlgorithm;
		this.size = size;

		this.creationDate = creationDate;
		this.modifiedDate = modifiedDate;
	}

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

	public void setHash(final byte[] hash) throws Exception {
		if (isDirectory) {
			throw new Exception("Directory hash may not be set, but must be calculated");
		} else {
			this.hash = hash;
		}
	}

	public String getName() {
		return name;
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public void addSubItem(final HashItem subItem) {
		hash = null;
		subItems.add(subItem);
	}

	/**
	 * Returns subitems in an unmodifiable list
	 */
	public List<HashItem> getSubItems() {
		return Collections.unmodifiableList(new ArrayList<>(subItems));
	}

	public long getSize() {
		if (isDirectory && size < 0) {
			size = 0;
			for (final HashItem subItem : subItems) {
				size += subItem.getSize();
			}
		}

		return size;
	}

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

	public LocalDateTime getCreationDate() {
		return creationDate;
	}

	public LocalDateTime getModifiedDate() {
		return modifiedDate;
	}

	public LocalDateTime getLatestFileCreation() {
		if (isDirectory) {
			LocalDateTime latestFileCreationDate = null;
			for (final HashItem subItem : subItems) {
				final LocalDateTime subItemLatestFileCreation = subItem.getLatestFileCreation();
				if (latestFileCreationDate == null) {
					latestFileCreationDate = subItemLatestFileCreation;
				} else if (subItemLatestFileCreation != null && latestFileCreationDate.isBefore(latestFileCreationDate)) {
					latestFileCreationDate = subItemLatestFileCreation;
				}
			}
			return latestFileCreationDate;
		} else {
			return creationDate;
		}
	}

	public LocalDateTime getLatestFileModified() {
		if (isDirectory) {
			LocalDateTime latestFileModifiedDate = null;
			for (final HashItem subItem : subItems) {
				final LocalDateTime subItemLatestFileModified = subItem.getLatestFileModified();
				if (latestFileModifiedDate == null) {
					latestFileModifiedDate = subItemLatestFileModified;
				} else if (subItemLatestFileModified != null && latestFileModifiedDate.isBefore(latestFileModifiedDate)) {
					latestFileModifiedDate = subItemLatestFileModified;
				}
			}
			return latestFileModifiedDate;
		} else {
			return modifiedDate;
		}
	}
}
