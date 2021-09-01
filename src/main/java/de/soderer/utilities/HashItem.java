package de.soderer.utilities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HashItem {
	private final String name;
	private final boolean isDirectory;
	private final HashAlgorithm hashAlgorithm;
	private final List<HashItem> subItems = new ArrayList<>();
	private byte[] hash = null;

	public HashItem(final String name, final boolean isDirectory, final HashAlgorithm hashAlgorithm) {
		this.name = name;
		this.isDirectory = isDirectory;
		this.hashAlgorithm = hashAlgorithm;
	}

	public byte[] getHash() throws Exception {
		if (isDirectory && hash == null) {
			final StringBuilder directoryContentData = new StringBuilder();
			for (final HashItem item : subItems) {
				directoryContentData.append(item.getName());
				directoryContentData.append(BitUtilities.toHexString(item.getHash()));
				directoryContentData.append("\n");
			}

			final MessageDigest messageDigest = hashAlgorithm.getMessageDigest();
			hash = messageDigest.digest(directoryContentData.toString().getBytes(StandardCharsets.UTF_8));
		}

		return hash;
	}

	public void setHash(final byte[] hash) {
		this.hash = hash;
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
}
