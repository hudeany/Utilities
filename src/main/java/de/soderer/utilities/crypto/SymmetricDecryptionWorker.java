package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

public class SymmetricDecryptionWorker extends WorkerSimple<Boolean> {
	public static final int EOF = -1;

	private InputStream encryptedDataInputStream = null;
	private OutputStream decryptedDataOutputStream = null;
	private char[] password = null;
	private String encryptionMethod = null;
	private long dataSizeHint = -1;

	private final int randomSaltSize = 16;

	/**
	 * Symmetric decrypt inputStream
	 *
	 * @param parent
	 * @param encryptedDataInputStream
	 * @param decryptedDataOutputStream
	 * @param password
	 */
	public SymmetricDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final char[] password) {
		this(parent, encryptedDataInputStream, decryptedDataOutputStream, password, null);
	}

	/**
	 * Symmetric decrypt inputStream
	 *
	 * @param parent
	 * @param encryptedDataInputStream
	 * @param decryptedDataOutputStream
	 * @param password
	 * @param encryptionMethod
	 */
	public SymmetricDecryptionWorker(final WorkerParentSimple parent, final InputStream encryptedDataInputStream, final OutputStream decryptedDataOutputStream, final char[] password, final String encryptionMethod) {
		super(parent);
		this.encryptedDataInputStream = encryptedDataInputStream;
		this.decryptedDataOutputStream = decryptedDataOutputStream;
		this.password = password;
		this.encryptionMethod = encryptionMethod;

		if (Utilities.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD;
		}
	}

	public void setDataSizeHint(final long dataSizeHint) {
		this.dataSizeHint = dataSizeHint;
	}

	/**
	 * Determine the correct key length in bits for the given cipher algorithm.
	 * Must mirror SymmetricEncryptionWorker.determineKeyLengthBits() exactly, otherwise
	 * decryption will derive a different key than was used for encryption.
	 *
	 * TODO: This mapping duplicates logic that should ideally live in CryptographicUtilities
	 * next to DEFAULT_SYMMETRIC_ENCRYPTION_METHOD, so both workers and any future callers
	 * share a single source of truth.
	 */
	private static int determineKeyLengthBits(final String encryptionMethod) {
		final String baseAlgorithm = encryptionMethod.contains("/") ? encryptionMethod.substring(0, encryptionMethod.indexOf('/')) : encryptionMethod;
		switch (baseAlgorithm) {
			case "aes":
				return 256;
			case "desede":
				return 192;
			case "des":
				return 64;
			case "blowfish":
				return 128;
			default:
				throw new IllegalArgumentException("Cannot determine key length for encryption method: " + encryptionMethod);
		}
	}

	@Override
	public Boolean work() throws Exception {
		if (encryptionMethod == null || "".equals(encryptionMethod.trim())) {
			throw new Exception("Empty symmetric decryption method");
		}
		if ("TripleDES".equalsIgnoreCase(encryptionMethod) || "3DES".equalsIgnoreCase(encryptionMethod)) {
			encryptionMethod = "DESede";
		}
		encryptionMethod = encryptionMethod.toLowerCase();

		signalProgress();

		itemsToDo = Math.max(dataSizeHint, encryptedDataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		byte[] salt = null;
		byte[] initializationVector = null;
		byte[] keyBytes = null;
		try (OutputStream outputStream = new BufferedOutputStream(decryptedDataOutputStream)) {
			try (InputStream inputStream = new BufferedInputStream(encryptedDataInputStream)) {
				salt = new byte[randomSaltSize];
				final int readSaltBytes = inputStream.read(salt);
				if (readSaltBytes != salt.length) {
					throw new Exception("Cannot read password salt prefix: Data is too short");
				}

				final Cipher symetricDecryptCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);
				keyBytes = CryptographicUtilities.stretchPassword(password, determineKeyLengthBits(encryptionMethod), salt);
				final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, encryptionMethod);

				initializationVector = new byte[symetricDecryptCipher.getBlockSize()];
				final int readIvBytes = inputStream.read(initializationVector);
				if (readIvBytes != initializationVector.length) {
					throw new Exception("Cannot read initialization vector prefix: Data is too short");
				}

				if (encryptionMethod.contains("gcm")) {
					// GCM verifies the authentication tag on the final read/doFinal, throwing if the data was tampered with
					symetricDecryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(128, initializationVector));
				} else {
					symetricDecryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
				}

				try (CipherInputStream cipherInputStream = new CipherInputStream(inputStream, symetricDecryptCipher)) {
					final int bufferSize = symetricDecryptCipher.getBlockSize() <= 0 ? 4096 : symetricDecryptCipher.getBlockSize();
					final byte[] buffer = new byte[bufferSize];
					int bytesRead;
					while ((bytesRead = cipherInputStream.read(buffer)) != EOF) {
						if (cancel) {
							break;
						} else {
							outputStream.write(buffer, 0, bytesRead);
							itemsDone += bytesRead;
							signalProgress();
						}
					}
					Utilities.clear(buffer);
				}

			}

			return true;
		} catch (final Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		} finally {
			Utilities.clear(initializationVector);
			Utilities.clear(salt);
			Utilities.clear(keyBytes);
		}
	}
}
