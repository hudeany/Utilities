package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.HashAlgorithm;
import de.soderer.utilities.HashItem;
import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.worker.WorkerDual;
import de.soderer.utilities.worker.WorkerParentDual;

public class DirectoryHashingWorker extends WorkerDual<HashItem> {
	private final File directoryToHash;
	private final HashAlgorithm hashAlgorithm;

	private String scanningForFilesMessage = "Scanning for files";

	public void setScanningForFilesMessage(final String scanningForFilesMessage) {
		this.scanningForFilesMessage = scanningForFilesMessage;
	}

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
			Thread.sleep(500);
			itemsToDo = FileUtilities.getAllSubfilesSize(directoryToHash);
			showProgress();
			return hashDirectory(directoryToHash);
		} catch (final Exception e) {
			throw new Exception("Error while hashing: " + e.getMessage(), e);
		}
	}

	public HashItem hashDirectory(final File directory) throws Exception {
		try {
			final HashItem item = new HashItem(directory.getName(), true, hashAlgorithm);
			final File[] subFiles = directory.listFiles();
			if (subFiles != null) {
				for (final File file : subFiles) {
					if (file.isDirectory()) {
						item.addSubItem(hashDirectory(file));
					} else {
						item.addSubItem(hashFile(file));
					}
				}
			}
			return item;
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
					break;
				} else {
					messageDigest.update(buffer, 0, bytesRead);
					subItemsDone += bytesRead;
					showItemProgress();
				}
			}

			final HashItem item = new HashItem(fileToHash.getName(), false, hashAlgorithm);
			item.setHash(messageDigest.digest());

			itemsDone += subItemsDone;
			showProgress();

			return item;
		} catch (final Exception e) {
			throw e;
		}
	}
}
