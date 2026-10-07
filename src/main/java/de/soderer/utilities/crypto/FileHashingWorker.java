package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import de.soderer.utilities.HashAlgorithm;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Worker that calculates the hash value of a file, reporting the progress in bytes.
 */
public class FileHashingWorker extends WorkerSimple<byte[]> {
	private final File fileToHash;
	private final HashAlgorithm hashAlgorithm;

	/**
	 * Creates a file hashing worker.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param fileToHash file to hash
	 * @param hashAlgorithm hash algorithm, null for SHA-256
	 */
	public FileHashingWorker(final WorkerParentSimple parent, final File fileToHash, final HashAlgorithm hashAlgorithm) {
		super(parent);

		this.fileToHash = fileToHash;

		if (hashAlgorithm == null) {
			// MD5 is broken for security purposes (practical collisions); SHA-256 is the safer default.
			this.hashAlgorithm = HashAlgorithm.SHA_256;
		} else {
			this.hashAlgorithm = hashAlgorithm;
		}

		itemsToDo = fileToHash.length();
	}

	/**
	 * Hashes the file.
	 *
	 * @return hash value, or null if the worker was cancelled
	 * @throws Exception if the file cannot be read
	 */
	@Override
	public byte[] work() throws Exception {
		signalProgress();

		try (InputStream inputStream = new BufferedInputStream(new FileInputStream(fileToHash))) {
			final MessageDigest messageDigest = MessageDigest.getInstance(hashAlgorithm.getAlgorithmName());

			final byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) >= 0) {
				if (cancel) {
					// The digest of partially read data would be a wrong hash value
					return null;
				} else {
					messageDigest.update(buffer, 0, bytesRead);
					itemsDone += bytesRead;
					signalProgress();
				}
			}

			return messageDigest.digest();
		} catch (final Exception e) {
			throw new Exception("Error while hashing: " + e.getMessage(), e);
		}
	}
}
