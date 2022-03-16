package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.HashAlgorithm;
import de.soderer.utilities.HashItem;
import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

public class DirectoryHashingWorker extends WorkerDual<HashItem> {
	private final File directoryToHash;
	private final HashAlgorithm hashAlgorithm;

	private boolean addDates = false;

	public boolean isAddDates() {
		return addDates;
	}

	public void setAddDates(final boolean addDates) {
		this.addDates = addDates;
	}

	private String scanningForFilesMessage = "Scanning for files";

	public void setScanningForFilesMessage(final String scanningForFilesMessage) {
		this.scanningForFilesMessage = scanningForFilesMessage;
	}

	// TODO: Compare with existing data json
	public DirectoryHashingWorker(final WorkerParentDual parent, final File directoryToHash, final HashAlgorithm hashAlgorithm) {
		super(parent);
		this.directoryToHash = directoryToHash;
		this.hashAlgorithm = hashAlgorithm;
	}

	@Override
	public HashItem work() throws Exception {
		showProgress();

		try {
			showItemStart(scanningForFilesMessage + " ...");
			showUnlimitedProgress();
			itemsToDo = FileUtilities.getAllSubfilesSize(directoryToHash);
			showProgress();
			return hashDirectory(directoryToHash);
		} catch (final Exception e) {
			throw new Exception("Error while hashing: " + e.getMessage(), e);
		}
	}

	public HashItem hashDirectory(final File directory) throws Exception {
		try {
			if (!directory.canRead()) {
				throw new Exception("Read access denied to: " + directory.getAbsolutePath());
			} else {
				HashItem item;
				if (!addDates) {
					item = new HashItem(directory.getName(), true, hashAlgorithm, -1);
				} else {
					try {
						final BasicFileAttributes attr = Files.readAttributes(Paths.get(directory.getAbsolutePath()), BasicFileAttributes.class);
						item = new HashItem(directory.getName(), true, hashAlgorithm, -1, new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
					} catch (@SuppressWarnings("unused") final Exception e) {
						item = new HashItem(directory.getName(), true, hashAlgorithm, -1);
					}
				}
				final File[] subFiles = directory.listFiles();
				if (subFiles != null) {
					Arrays.sort(subFiles);
					for (final File file : subFiles) {
						if (cancel) {
							return null;
						} else {
							if (file.isDirectory()) {
								item.addSubItem(hashDirectory(file));
							} else {
								item.addSubItem(hashFile(file));
							}
						}
					}
				}
				if (cancel) {
					return null;
				} else {
					return item;
				}
			}
		} catch (final Exception e) {
			throw e;
		}
	}

	private HashItem hashFile(final File fileToHash) throws Exception {
		subItemsToDo = fileToHash.length();
		subItemsDone = 0;
		showProgress();
		showItemStart(TextUtilities.trimStringToMaximumLength(fileToHash.getAbsolutePath(), 60, " ... "));
		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(fileToHash))) {
			final MessageDigest messageDigest = hashAlgorithm.getMessageDigest();

			final byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) >= 0) {
				if (cancel) {
					return null;
				} else {
					messageDigest.update(buffer, 0, bytesRead);
					subItemsDone += bytesRead;
					showItemProgress();
				}
			}

			HashItem item;
			if (!addDates) {
				item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
			} else {
				try {
					final BasicFileAttributes attr = Files.readAttributes(Paths.get(fileToHash.getAbsolutePath()), BasicFileAttributes.class);
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length(), new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
				} catch (@SuppressWarnings("unused") final Exception e) {
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
				}
			}
			item.setHash(messageDigest.digest());

			itemsDone += subItemsDone;
			showProgress();

			return item;
		} catch (@SuppressWarnings("unused") final Exception e) {
			HashItem item;
			if (!addDates) {
				item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
			} else {
				try {
					final BasicFileAttributes attr = Files.readAttributes(Paths.get(fileToHash.getAbsolutePath()), BasicFileAttributes.class);
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length(), new Date(attr.creationTime().toMillis()), new Date(attr.lastModifiedTime().toMillis()));
				} catch (@SuppressWarnings("unused") final IOException e1) {
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
				}
			}
			item.setHash(null);

			itemsDone += fileToHash.length();
			showProgress();

			return item;
		}
	}
}
