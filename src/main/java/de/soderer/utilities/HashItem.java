package de.soderer.utilities;

import java.security.MessageDigest;
import java.util.ArrayList;
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
		if (isDirectory) {
			final StringBuilder directoryContentData = new StringBuilder();
			for (final HashItem item : subItems) {
				directoryContentData.append(item.getName());
				directoryContentData.append(BitUtilities.getHexString(item.getHash()));
				directoryContentData.append("\n");
			}

			final MessageDigest messageDigest = hashAlgorithm.getMessageDigest();
			return messageDigest.digest(directoryContentData.toString().getBytes("UTF-8"));
		} else {
			return hash;
		}
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

	public List<HashItem> getSubItems() {
		return subItems;
	}
}
