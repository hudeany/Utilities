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

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.HashAlgorithm;
import de.soderer.utilities.HashItem;
import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

/**
 * Worker that recursively hashes all files of a directory and returns a tree of {@link HashItem}s.<br>
 * Files that cannot be read are included with a null hash value.
 */
public class DirectoryHashingWorker extends WorkerDual<HashItem> {
	private final File directoryToHash;
	private final HashAlgorithm hashAlgorithm;

	private boolean addDates = false;

	/**
	 * Returns whether the creation and modification dates are added to the hash items.
	 *
	 * @return true if dates are added
	 */
	public boolean isAddDates() {
		return addDates;
	}

	/**
	 * Sets whether the creation and modification dates are added to the hash items.
	 *
	 * @param addDates true to add dates
	 */
	public void setAddDates(final boolean addDates) {
		this.addDates = addDates;
	}

	private String scanningForFilesMessage = "Scanning for files";

	/**
	 * Sets the message which is shown while scanning for the files to hash.
	 *
	 * @param scanningForFilesMessage message text
	 */
	public void setScanningForFilesMessage(final String scanningForFilesMessage) {
		this.scanningForFilesMessage = scanningForFilesMessage;
	}

	// TODO: Compare with existing data json
	/**
	 * Creates a directory hashing worker.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param directoryToHash directory to hash
	 * @param hashAlgorithm hash algorithm
	 */
	public DirectoryHashingWorker(final WorkerParentDual parent, final File directoryToHash, final HashAlgorithm hashAlgorithm) {
		super(parent);
		this.directoryToHash = directoryToHash;
		this.hashAlgorithm = hashAlgorithm;
	}

	/**
	 * Hashes the directory.
	 *
	 * @return hash item tree of the directory, or null if the worker was cancelled
	 * @throws Exception if the directory cannot be read
	 */
	@Override
	public HashItem work() throws Exception {
		signalProgress();

		try {
			signalItemStart(scanningForFilesMessage + " ...", null);
			signalUnlimitedProgress();
			itemsToDo = FileUtilities.getAllSubfilesSize(directoryToHash);
			signalProgress();
			return hashDirectory(directoryToHash);
		} catch (final Exception e) {
			throw new Exception("Error while hashing: " + e.getMessage(), e);
		}
	}

	/**
	 * Recursively hashes a directory.
	 *
	 * @param directory directory to hash
	 * @return hash item of the directory with its sub items, or null if the worker was cancelled
	 * @throws Exception if the directory cannot be read
	 */
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
						item = new HashItem(directory.getName(), true, hashAlgorithm, -1, DateUtilities.getLocalDateTime(attr.creationTime().toMillis()), DateUtilities.getLocalDateTime(attr.lastModifiedTime().toMillis()));
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
		signalProgress();
		signalItemStart(TextUtilities.trimStringToMaximumLength(fileToHash.getAbsolutePath(), 60, " ... "), null);
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
					signalItemProgress();
				}
			}

			HashItem item;
			if (!addDates) {
				item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
			} else {
				try {
					final BasicFileAttributes attr = Files.readAttributes(Paths.get(fileToHash.getAbsolutePath()), BasicFileAttributes.class);
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length(), DateUtilities.getLocalDateTime(attr.creationTime().toMillis()), DateUtilities.getLocalDateTime(attr.lastModifiedTime().toMillis()));
				} catch (@SuppressWarnings("unused") final Exception e) {
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
				}
			}
			item.setHash(messageDigest.digest());

			itemsDone += subItemsDone;
			signalProgress();

			return item;
		} catch (final Exception e) {
			e.printStackTrace();

			HashItem item;
			if (!addDates) {
				item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
			} else {
				try {
					final BasicFileAttributes attr = Files.readAttributes(Paths.get(fileToHash.getAbsolutePath()), BasicFileAttributes.class);
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length(), DateUtilities.getLocalDateTime(attr.creationTime().toMillis()), DateUtilities.getLocalDateTime(attr.lastModifiedTime().toMillis()));
				} catch (@SuppressWarnings("unused") final IOException e1) {
					item = new HashItem(fileToHash.getName(), false, hashAlgorithm, fileToHash.length());
				}
			}
			item.setHash(null);

			itemsDone += fileToHash.length();
			signalProgress();

			return item;
		}
	}
}
