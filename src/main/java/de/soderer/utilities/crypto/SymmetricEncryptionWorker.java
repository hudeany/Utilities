package de.soderer.utilities.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.worker.WorkerParentSimple;
import de.soderer.utilities.worker.WorkerSimple;

/**
 * Worker that encrypts a stream with a password.<br>
 * The key is derived with PBKDF2 from the password and a 16 byte random salt. The output consists of the salt,
 * the random initialization vector and the encrypted data. Both streams are closed after the work.
 */
public class SymmetricEncryptionWorker extends WorkerSimple<Boolean> {
	/**
	 * Return value of stream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	private InputStream dataInputStream = null;
	private OutputStream encryptedDataOutputStream = null;
	private char[] password = null;
	private String encryptionMethod = null;
	private long dataSizeHint = -1;

	private final int randomSaltSize = 16;

	/**
	 * Symmetric encrypt inputStream with the default method {@link CryptographicUtilities#DEFAULT_SYMMETRIC_ENCRYPTION_METHOD}.
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the data to encrypt
	 * @param encryptedDataOutputStream stream to write the encrypted data to
	 * @param password password to derive the key from
	 */
	public SymmetricEncryptionWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final OutputStream encryptedDataOutputStream, final char[] password) {
		this(parent, dataInputStream, encryptedDataOutputStream, password, null);
	}

	/**
	 * Symmetric encrypt inputStream
	 *
	 * @param parent parent to report progress to, may be null
	 * @param dataInputStream stream with the data to encrypt
	 * @param encryptedDataOutputStream stream to write the encrypted data to
	 * @param password password to derive the key from
	 * @param encryptionMethod cipher transformation (e.g. "AES/CBC/PKCS7Padding" or "AES/GCM/NoPadding"),
	 *        blank for {@link CryptographicUtilities#DEFAULT_SYMMETRIC_ENCRYPTION_METHOD}
	 */
	public SymmetricEncryptionWorker(final WorkerParentSimple parent, final InputStream dataInputStream, final OutputStream encryptedDataOutputStream, final char[] password, final String encryptionMethod) {
		super(parent);
		this.dataInputStream = dataInputStream;
		this.encryptedDataOutputStream = encryptedDataOutputStream;
		this.password = password;
		this.encryptionMethod = encryptionMethod;

		if (Utilities.isBlank(this.encryptionMethod)) {
			this.encryptionMethod = CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD;
		}
	}

	/**
	 * Sets the expected data size in bytes for the progress display, if the stream cannot tell its size.
	 *
	 * @param dataSizeHint expected data size in bytes
	 */
	public void setDataSizeHint(final long dataSizeHint) {
		this.dataSizeHint = dataSizeHint;
	}

	@Override
	public Boolean work() throws Exception {
		if (encryptionMethod == null || "".equals(encryptionMethod.trim())) {
			throw new Exception("Empty symmetric encryption method");
		}
		if ("TripleDES".equalsIgnoreCase(encryptionMethod) || "3DES".equalsIgnoreCase(encryptionMethod)) {
			encryptionMethod = "DESede";
		}
		encryptionMethod = encryptionMethod.toLowerCase(Locale.ROOT);

		signalProgress();

		itemsToDo = Math.max(dataSizeHint, dataInputStream.available());

		Security.addProvider(new BouncyCastleProvider());

		byte[] salt = null;
		byte[] initializationVector = null;
		byte[] keyBytes = null;
		try (InputStream inputStream = new BufferedInputStream(dataInputStream)) {
			try (OutputStream outputStream = new BufferedOutputStream(encryptedDataOutputStream)) {
				salt = new byte[randomSaltSize];
				new SecureRandom().nextBytes(salt);
				outputStream.write(salt);

				keyBytes = CryptographicUtilities.stretchPassword(password, CryptographicUtilities.determineSymmetricKeyLengthBits(encryptionMethod), salt);
				final SecretKeySpec keySpec = new SecretKeySpec(keyBytes, encryptionMethod);
				final Cipher symetricEncryptCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);

				initializationVector = new byte[symetricEncryptCipher.getBlockSize()];
				new SecureRandom().nextBytes(initializationVector);
				outputStream.write(initializationVector);

				if (encryptionMethod.contains("gcm")) {
					// GCM provides authenticated encryption (detects tampering), unlike plain CBC
					symetricEncryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(128, initializationVector));
				} else {
					symetricEncryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
				}

				// CipherOutputStream must be closed at end for correct final block data
				try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, symetricEncryptCipher)) {
					final int bufferSize = symetricEncryptCipher.getBlockSize() <= 0 ? 4096 : symetricEncryptCipher.getBlockSize();
					final byte[] buffer = new byte[bufferSize];
					int bytesRead;
					while ((bytesRead = inputStream.read(buffer)) != EOF) {
						if (cancel) {
							break;
						} else {
							cipherOutputStream.write(buffer, 0, bytesRead);
							itemsDone += bytesRead;
							signalProgress();
						}
					}
					Utilities.clear(buffer);
				}

				return true;
			}
		} catch (final Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		} finally {
			Utilities.clear(initializationVector);
			Utilities.clear(salt);
			Utilities.clear(keyBytes);
		}
	}
}